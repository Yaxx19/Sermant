/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.discovery.interceptors;

import io.sermant.core.config.ConfigManager;
import io.sermant.core.operation.OperationManager;
import io.sermant.core.operation.converter.api.YamlConverter;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.plugin.config.ServiceMeta;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.discovery.config.DiscoveryPluginConfig;
import io.sermant.discovery.entity.PlugEffectStrategyCache;
import io.sermant.implement.operation.converter.YamlConverterImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

/**
 * Basic test
 *
 * @author chengyouling
 * @since 2022-10-09
 */
public class BaseTest {
    protected final DiscoveryPluginConfig discoveryPluginConfig = new DiscoveryPluginConfig();

    protected final YamlConverter yamlConverter = new YamlConverterImpl();

    protected final ServiceMeta serviceMeta = new ServiceMeta();

    protected MockedStatic<PluginServiceManager> pluginServiceManagerMockedStatic;

    protected MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    protected MockedStatic<OperationManager> operationManagerMockedStatic;

    protected MockedStatic<ConfigManager> configManagerMockedStatic;

    @Before
    public void setUp() {
        pluginConfigManagerMockedStatic = Mockito
                .mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(DiscoveryPluginConfig.class))
                .thenReturn(discoveryPluginConfig);
        pluginServiceManagerMockedStatic = Mockito
                .mockStatic(PluginServiceManager.class);
        operationManagerMockedStatic = Mockito
                .mockStatic(OperationManager.class);
        operationManagerMockedStatic.when(() -> OperationManager.getOperation(YamlConverter.class))
                .thenReturn(yamlConverter);
        configManagerMockedStatic = Mockito
                .mockStatic(ConfigManager.class);
        configManagerMockedStatic.when(() -> ConfigManager.getConfig(ServiceMeta.class))
                .thenReturn(serviceMeta);
    }

    @After
    public void tearDown() {
        clearCache();
        pluginConfigManagerMockedStatic.close();
        pluginServiceManagerMockedStatic.close();
        operationManagerMockedStatic.close();
        configManagerMockedStatic.close();
    }

    private void clearCache() {
        final Optional<Object> dynamicConfig = ReflectUtils.getFieldValue(PlugEffectStrategyCache.INSTANCE, "caches");
        Assert.assertTrue(dynamicConfig.isPresent() && dynamicConfig.get() instanceof Map);
        ((Map) dynamicConfig.get()).clear();
    }
}