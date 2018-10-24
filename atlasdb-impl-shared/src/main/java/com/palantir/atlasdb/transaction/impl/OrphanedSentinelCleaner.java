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

package com.palantir.atlasdb.transaction.impl;

import java.util.Set;

import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.transaction.api.ConflictHandler;
import com.palantir.atlasdb.transaction.api.Transaction;

public interface OrphanedSentinelCleaner {
    void enqueue(Transaction txn, TableReference table, Set<Cell> cells);

    static OrphanedSentinelCleaner noOp() {
        return (txn, table, cells) -> {};
    }

    static OrphanedSentinelCleaner deleting(ConflictDetectionManager conflictHandlers) {
        return (txn, table, cells) -> {
            ConflictHandler conflictHandler = conflictHandlers.get(table);
            if (conflictHandler != null && conflictHandler.checkWriteWriteConflicts()) {
                txn.delete(table, cells);
            }
        };
    }
}
