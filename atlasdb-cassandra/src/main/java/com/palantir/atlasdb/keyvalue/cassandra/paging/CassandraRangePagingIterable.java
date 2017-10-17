/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.keyvalue.cassandra.paging;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;

import com.google.common.base.Supplier;
import com.palantir.atlasdb.keyvalue.api.ColumnSelection;
import com.palantir.atlasdb.keyvalue.api.RangeRequest;
import com.palantir.atlasdb.keyvalue.api.RowResult;
import com.palantir.atlasdb.keyvalue.cassandra.ResultsExtractor;
import com.palantir.util.paging.AbstractPagingIterable;
import com.palantir.util.paging.SimpleTokenBackedResultsPage;
import com.palantir.util.paging.TokenBackedBasicResultsPage;

public class CassandraRangePagingIterable<T>
        extends AbstractPagingIterable<RowResult<T>, TokenBackedBasicResultsPage<RowResult<T>, byte[]>> {
    private final ColumnGetter columnGetter;
    private final RangeRequest rangeRequest;
    private final Supplier<ResultsExtractor<T>> resultsExtractor;
    private final long timestamp;

    private final int batchHint;
    private final ColumnSelection selection;
    private final RowRangeLoader rowRangeLoader;
    private final ColumnFetchMode fetchMode;

    public CassandraRangePagingIterable(
            RowRangeLoader rowRangeLoader,
            ColumnFetchMode fetchMode,
            ColumnGetter columnGetter,
            RangeRequest rangeRequest,
            Supplier<ResultsExtractor<T>> resultsExtractor,
            long timestamp) {
        this.rowRangeLoader = rowRangeLoader;
        this.fetchMode = fetchMode;
        this.columnGetter = columnGetter;
        this.rangeRequest = rangeRequest;
        this.resultsExtractor = resultsExtractor;
        this.timestamp = timestamp;

        batchHint = rangeRequest.getBatchHint() == null ? 100 : rangeRequest.getBatchHint();
        selection = rangeRequest.getColumnNames().isEmpty() ? ColumnSelection.all()
                : ColumnSelection.create(rangeRequest.getColumnNames());
    }

    @Override
    protected TokenBackedBasicResultsPage<RowResult<T>, byte[]> getFirstPage() throws Exception {
        return getSinglePage(rangeRequest.getStartInclusive());
    }

    @Override
    protected TokenBackedBasicResultsPage<RowResult<T>, byte[]> getNextPage(
            TokenBackedBasicResultsPage<RowResult<T>, byte[]> previous)
            throws Exception {
        return getSinglePage(previous.getTokenForNextPage());
    }

    private TokenBackedBasicResultsPage<RowResult<T>, byte[]> getSinglePage(byte[] startKey) throws Exception {
        List<KeySlice> rows = getRows(startKey);
        Map<ByteBuffer, List<ColumnOrSuperColumn>> columnsByRow = getColumns(rows);
        TokenBackedBasicResultsPage<RowResult<T>, byte[]> page = getPage(columnsByRow);

        if (page.moreResultsAvailable() && pageShouldBeLastPage(rows)) {
            return pageWithNoMoreResultsAvailable(page);
        }

        return page;
    }

    private List<KeySlice> getRows(byte[] startKey) throws Exception {
        KeyRange keyRange = KeyRanges.createKeyRange(startKey, rangeRequest.getEndExclusive(), batchHint);
        return rowRangeLoader.getRows(keyRange, fetchMode);
    }

    private Map<ByteBuffer, List<ColumnOrSuperColumn>> getColumns(List<KeySlice> firstPage) {
        return columnGetter.getColumnsByRow(firstPage);
    }

    private TokenBackedBasicResultsPage<RowResult<T>, byte[]> getPage(
            Map<ByteBuffer, List<ColumnOrSuperColumn>> colsByKey) {
        return resultsExtractor.get().getPageFromRangeResults(
                colsByKey, timestamp, selection, rangeRequest.getEndExclusive());
    }

    private boolean pageShouldBeLastPage(List<KeySlice> rows) {
        // If get_range_slices didn't return the full number of results, there's no
        // point to trying to get another page
        return rows.size() < batchHint;
    }

    private TokenBackedBasicResultsPage<RowResult<T>, byte[]> pageWithNoMoreResultsAvailable(
            TokenBackedBasicResultsPage<RowResult<T>, byte[]> page) {
        return SimpleTokenBackedResultsPage.create(rangeRequest.getEndExclusive(), page.getResults(), false);
    }

}