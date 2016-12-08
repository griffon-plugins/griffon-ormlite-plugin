/*
 * Copyright 2014-2016 the original author or authors.
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

import griffon.core.Configuration;
import griffon.core.addon.GriffonAddon;
import griffon.core.injection.Module;
import griffon.plugins.ormlite.ConnectionSourceFactory;
import griffon.plugins.ormlite.ConnectionSourceHandler;
import griffon.plugins.ormlite.ConnectionSourceStorage;
import org.codehaus.griffon.runtime.core.injection.AbstractModule;
import org.codehaus.griffon.runtime.util.ResourceBundleProvider;
import org.kordamp.jipsy.ServiceProviderFor;

import javax.inject.Named;
import java.util.ResourceBundle;

import static griffon.util.AnnotationUtils.named;

/**
 * @author Andres Almiray
 */
@Named("ormlite")
@ServiceProviderFor(Module.class)
public class OrmliteModule extends AbstractModule {
    @Override
    protected void doConfigure() {
        // tag::bindings[]
        bind(ResourceBundle.class)
            .withClassifier(named("ormlite"))
            .toProvider(new ResourceBundleProvider("Ormlite"))
            .asSingleton();

        bind(Configuration.class)
            .withClassifier(named("ormlite"))
            .to(DefaultOrmliteConfiguration.class)
            .asSingleton();

        bind(ConnectionSourceStorage.class)
            .to(DefaultConnectionSourceStorage.class)
            .asSingleton();

        bind(ConnectionSourceFactory.class)
            .to(DefaultConnectionSourceFactory.class)
            .asSingleton();

        bind(ConnectionSourceHandler.class)
            .to(DefaultConnectionSourceHandler.class)
            .asSingleton();

        bind(GriffonAddon.class)
            .to(OrmliteAddon.class)
            .asSingleton();
        // end::bindings[]
    }
}
