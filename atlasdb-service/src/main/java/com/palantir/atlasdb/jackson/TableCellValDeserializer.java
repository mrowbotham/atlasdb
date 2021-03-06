/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.jackson;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.palantir.atlasdb.api.TableCellVal;
import com.palantir.atlasdb.impl.TableMetadataCache;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.table.description.TableMetadata;

public class TableCellValDeserializer extends StdDeserializer<TableCellVal> {
    private static final long serialVersionUID = 1L;
    private final TableMetadataCache metadataCache;

    protected TableCellValDeserializer(TableMetadataCache metadataCache) {
        super(TableCellVal.class);
        this.metadataCache = metadataCache;
    }

    @Override
    public TableCellVal deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.readValueAsTree();
        String tableName = node.get("table").textValue();
        TableMetadata metadata = metadataCache.getMetadata(tableName);
        Map<Cell, byte[]> values = AtlasDeserializers.deserializeCellVals(metadata, node.get("data"));
        return new TableCellVal(tableName, values);
    }
}
