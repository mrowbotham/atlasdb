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

package com.palantir.atlasdb.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.spi.AtlasDbFactory;
import com.palantir.timestamp.TimestampService;

public class InMemoryAtlasDbFactoryTest {
    private final AtlasDbFactory factory = new InMemoryAtlasDbFactory();

    @Test
    public void syncInitKvsSynchronous() {
        KeyValueService kvs = createRawKeyValueService(false);
        assertThat(kvs.isInitialized()).isTrue();
        assertThat(kvs.getAllTableNames()).isEmpty();
    }

    @Test
    public void asyncInitKvsSynchronous() {
        KeyValueService kvs = createRawKeyValueService(true);
        assertThat(kvs.isInitialized()).isTrue();
        assertThat(kvs.getAllTableNames()).isEmpty();
    }

    @Test
    public void syncInitTimestampServiceSynchronous() {
        TimestampService timestampService = factory.createTimestampService(null, Optional.empty(), false);
        assertThat(timestampService.isInitialized()).isTrue();
        assertThat(timestampService.getFreshTimestamp()).isEqualTo(1L);
    }

    @Test
    public void asyncInitTimestampServiceSynchronous() {
        KeyValueService kvs = createRawKeyValueService(true);
        TimestampService timestampService = factory.createTimestampService(kvs, Optional.empty(), true);
        assertThat(timestampService.isInitialized()).isTrue();
        assertThat(timestampService.getFreshTimestamp()).isEqualTo(1L);
    }

    private KeyValueService createRawKeyValueService(boolean initializeAsync) {
        return factory.createRawKeyValueService(
                null,
                null,
                Optional::empty,
                null,
                Optional.empty(),
                null,
                initializeAsync);
    }
}
