/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2020 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.ormlite;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import griffon.annotations.core.Nonnull;

import java.io.IOException;

/**
 * @author Andres Almiray
 */
public class LinkedDatabaseConnection extends DatabaseConnectionDecorator {
    private RecordingConnectionSource connectionSource;

    public LinkedDatabaseConnection(@Nonnull DatabaseConnection delegate, @Nonnull RecordingConnectionSource connectionSource) {
        super(delegate);
        this.connectionSource = connectionSource;
    }

    @Nonnull
    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    @Override
    public void close() throws IOException {
        super.close();
        connectionSource.decreaseConnectionCount();
    }

    public void unlink() {
        connectionSource.decreaseConnectionCount();
    }
}
