/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.core.plugin.agent.enhance;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClassLoader loadClass interceptor
 *
 * @author luanwenfei
 * @since 2023-04-28
 */
public class ClassLoaderLoadClassInterceptor extends AbstractClassLoaderInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        String name = (String) context.getArguments()[0];
        if (isSermantClass(name)) {
            try {
                Class<?> sermantClazz = ClassLoaderManager.getPluginClassFinder().loadSermantClass(name);
                LOGGER.log(Level.INFO, "Load class: {0} successfully by sermant.", name);
                context.changeResult(sermantClazz);
                context.changeThrowable(null);
            } catch (ClassNotFoundException classNotFoundException) {
                LOGGER.log(Level.WARNING, "Class can not load class by sermant. ", classNotFoundException.getMessage());
            }
        }
        return context;
    }
}