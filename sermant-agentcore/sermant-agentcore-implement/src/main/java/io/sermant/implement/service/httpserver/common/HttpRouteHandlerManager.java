/*
 * Copyright (C) 2024-2024 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.implement.service.httpserver.common;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.plugin.Plugin;
import io.sermant.core.plugin.PluginManager;
import io.sermant.core.service.httpserver.annotation.HttpRouteMapping;
import io.sermant.core.service.httpserver.api.HttpRequest;
import io.sermant.core.service.httpserver.api.HttpRouteHandler;
import io.sermant.core.service.httpserver.exception.HttpServerException;
import io.sermant.core.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 路由处理器管理
 *
 * @author zwmagic
 * @since 2024-02-02
 */
public class HttpRouteHandlerManager {
    private static final String SERMANT_PLUGIN_NAME = "sermant";

    private static final int HTTP_PATH_SIZE = 3;

    private static final HttpRouteHandlerManager INSTANCE = new HttpRouteHandlerManager();

    private static final Map<String, List<HttpRouter>> ROUTERS_MAPPING = new ConcurrentHashMap<>();

    private HttpRouteHandlerManager() {
    }

    /**
     * 获取与给定请求对应的HttpRouteHandler。
     *
     * @param request 请求对象
     * @return 包含HttpRouteHandler的Optional对象，如果不存在对应的HttpRouteHandler则返回空的Optional对象
     */
    public static Optional<HttpRouteHandler> getHandler(HttpRequest request) {
        String pluginName = INSTANCE.getPluginName(request);
        List<HttpRouter> routers = INSTANCE.getRouteHandlers(pluginName);
        if (CollectionUtils.isEmpty(routers)) {
            return Optional.empty();
        }
        for (HttpRouter router : routers) {
            if (router.match(request)) {
                return Optional.of(router.getHandler());
            }
        }
        return Optional.empty();
    }

    private List<HttpRouter> getRouteHandlers(String pluginName) {
        List<HttpRouter> routers = ROUTERS_MAPPING.get(pluginName);
        if (routers != null) {
            return routers;
        }
        synchronized (ROUTERS_MAPPING) {
            if (routers != null) {
                return routers;
            }
            ClassLoader classLoader;
            if (SERMANT_PLUGIN_NAME.equals(pluginName)) {
                // sermant core 支持对外提供http api 能力
                classLoader = ClassLoaderManager.getFrameworkClassLoader();
            } else {
                Plugin plugin = PluginManager.getPluginMap().get(pluginName);
                if (plugin == null) {
                    return Collections.emptyList();
                }
                classLoader = plugin.getServiceClassLoader() != null ? plugin.getServiceClassLoader()
                        : plugin.getPluginClassLoader();
            }
            addRouteHandlers(pluginName, classLoader);
            routers = ROUTERS_MAPPING.get(pluginName);
        }

        return routers;
    }

    private void addRouteHandlers(String pluginName, ClassLoader classLoader) {
        for (HttpRouteHandler handler : ServiceLoader.load(HttpRouteHandler.class, classLoader)) {
            HttpRouteMapping annotation = handler.getClass().getAnnotation(HttpRouteMapping.class);
            if (annotation == null) {
                continue;
            }
            List<HttpRouter> routers = ROUTERS_MAPPING.computeIfAbsent(pluginName, list -> new ArrayList<>());
            routers.add(new HttpRouter(pluginName, handler, annotation));
        }
    }

    private String getPluginName(HttpRequest request) {
        String path = request.getPath();
        String[] array = path.split("/");
        if (array.length < HTTP_PATH_SIZE) {
            throw new HttpServerException(HttpCodeEnum.BAD_REQUEST.getCode(),
                    "Bad Request: The format of the requested path [" + request.getOriginalPath() + "] is incorrect");
        }
        return array[1];
    }
}
