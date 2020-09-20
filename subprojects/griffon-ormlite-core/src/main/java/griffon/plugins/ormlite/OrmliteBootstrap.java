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
package griffon.plugins.ormlite;

import com.j256.ormlite.support.ConnectionSource;
import griffon.annotations.core.Nonnull;

/**
 * @author Andres Almiray
 */
public interface OrmliteBootstrap {
    void init(@Nonnull String sessionFactoryName, @Nonnull ConnectionSource connectionSource);

    void destroy(@Nonnull String sessionFactoryName, @Nonnull ConnectionSource connectionSource);
}