/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2021 The author and/or original authors.
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
package griffon.plugins.ormlite.exceptions;

import griffon.annotations.core.Nonnull;
import griffon.exceptions.GriffonException;

import java.sql.SQLException;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class RuntimeSQLException extends GriffonException {
    private final String databaseName;

    public RuntimeSQLException(@Nonnull String databaseName, @Nonnull SQLException sqle) {
        super(format(databaseName), requireNonNull(sqle, "sqle"));
        this.databaseName = databaseName;
    }

    @Nonnull
    private static String format(@Nonnull String databaseName) {
        requireNonBlank(databaseName, "databaseName");
        return "An error occurred when executing a statement on database '" + databaseName + "'";
    }

    @Nonnull
    public String getDatabaseName() {
        return databaseName;
    }
}
