/*
 * Copyright 2014-2017 the original author or authors.
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
package org.codehaus.griffon.runtime.ormlite;


import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andres Almiray
 */
public class RecordingConnectionSource extends ConnectionSourceDecorator {
    private AtomicInteger connectionCount = new AtomicInteger(0);

    public RecordingConnectionSource(@Nonnull ConnectionSource delegate) {
        super(delegate);
    }

    public int increaseConnectionCount() {
        return connectionCount.incrementAndGet();
    }

    public int decreaseConnectionCount() {
        return connectionCount.decrementAndGet();
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }

    @Override
    public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
        DatabaseConnection connection = super.getReadOnlyConnection(tableName);
        increaseConnectionCount();
        return wrap(connection);
    }

    @Override
    public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
        DatabaseConnection connection = super.getReadWriteConnection(tableName);
        increaseConnectionCount();
        return wrap(connection);
    }

    @Override
    public DatabaseConnection getSpecialConnection(String tableName) {
        DatabaseConnection connection = super.getSpecialConnection(tableName);
        increaseConnectionCount();
        return wrap(connection);
    }

    @Override
    public void releaseConnection(DatabaseConnection connection) throws SQLException {
        if (connection instanceof LinkedDatabaseConnection) {
            LinkedDatabaseConnection ldb = (LinkedDatabaseConnection) connection;
            super.releaseConnection(ldb.getDelegate());
            ldb.unlink();
        } else {
            super.releaseConnection(connection);
        }
    }

    @Nonnull
    private DatabaseConnection wrap(@Nonnull DatabaseConnection connection) {
        return connection instanceof LinkedDatabaseConnection ? connection : new LinkedDatabaseConnection(connection, this);
    }
}
