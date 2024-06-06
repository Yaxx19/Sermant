/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.registry.service.client;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.StringUtils;
import io.sermant.registry.config.NacosRegisterConfig;
import io.sermant.registry.context.RegisterContext;
import io.sermant.registry.service.cache.ServiceCache;
import io.sermant.registry.service.register.NacosServiceInstance;
import io.sermant.registry.service.register.NacosServiceManager;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nacos registers the implementation client
 *
 * @since 2022-11-10
 */
public class NacosClient {
    private static final String STATUS_UP = "UP";

    private static final String STATUS_DOWN = "DOWN";

    private static final String STATUS_UNKNOW = "UNKNOW";

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final NacosServiceManager nacosServiceManager;

    private final NacosRegisterConfig nacosRegisterConfig;

    private final NacosServiceDiscovery nacosServiceDiscovery;

    private Instance instance;

    /**
     * Constructor
     */
    public NacosClient() {
        nacosRegisterConfig = PluginConfigManager.getPluginConfig(NacosRegisterConfig.class);
        nacosServiceManager = new NacosServiceManager();
        nacosServiceDiscovery = new NacosServiceDiscovery(nacosServiceManager);
    }

    /**
     * Microservice registration
     */
    public void register() {
        if (StringUtils.isEmpty(RegisterContext.INSTANCE.getClientInfo().getServiceId())) {
            LOGGER.warning("No service to register for nacos client...");
            return;
        }
        String serviceId = RegisterContext.INSTANCE.getClientInfo().getServiceId();
        String group = nacosRegisterConfig.getGroup();
        instance = nacosServiceManager.buildNacosInstanceFromRegistration();
        try {
            NamingService namingService = nacosServiceManager.getNamingService();
            namingService.registerInstance(serviceId, group, instance);
            LOGGER.log(Level.INFO, String.format(Locale.ENGLISH, "registry success, group={%s},serviceId={%s},"
                    + "instanceIp={%s},instancePort={%s} register finished", group, serviceId, instance.getIp(),
                    instance.getPort()));
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "failed when registry service，serviceId={%s}",
                    serviceId), e);
        }
    }

    /**
     * Offline processing
     */
    public void deregister() {
        if (StringUtils.isEmpty(RegisterContext.INSTANCE.getClientInfo().getServiceId())) {
            LOGGER.warning("No service to de-register for nacos client...");
            return;
        }
        String serviceId = RegisterContext.INSTANCE.getClientInfo().getServiceId();
        String group = nacosRegisterConfig.getGroup();
        try {
            NamingService namingService = nacosServiceManager.getNamingService();
            namingService.deregisterInstance(serviceId, group, instance);
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "failed when deRegister service，"
                    + "serviceId={%s}", serviceId), e);
        }
    }

    /**
     * Obtain the NACOS service status
     *
     * @return Service status
     */
    public String getServerStatus() {
        try {
            NamingService namingService = nacosServiceManager.getNamingService();
            return namingService.getServerStatus();
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, "get nacos server status failed", e);
        }
        return STATUS_UNKNOW;
    }

    /**
     * Update the microservice instance status
     *
     * @param status Service status
     */
    public void updateInstanceStatus(String status) {
        if (!STATUS_UP.equalsIgnoreCase(status) && !STATUS_DOWN.equalsIgnoreCase(status)) {
            LOGGER.warning(String.format(Locale.ENGLISH,"can't support status={%s},"
                    + "please choose UP or DOWN", status));
            return;
        }
        String serviceId = RegisterContext.INSTANCE.getClientInfo().getServiceId();
        String group = nacosRegisterConfig.getGroup();
        Instance updateInstance = nacosServiceManager.buildNacosInstanceFromRegistration();
        updateInstance.setEnabled(!STATUS_DOWN.equalsIgnoreCase(status));
        try {
            nacosServiceManager.getNamingMaintainService().updateInstance(serviceId, group, updateInstance);
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "update nacos instance status failed,"
                    + "serviceId={%s}", serviceId), e);
        }
    }

    /**
     * Get the microservice instance status
     *
     * @return Instance status
     */
    public String getInstanceStatus() {
        String serviceId = RegisterContext.INSTANCE.getClientInfo().getServiceId();
        String group = nacosRegisterConfig.getGroup();
        try {
            NamingService namingService = nacosServiceManager.getNamingService();
            List<Instance> instances = namingService.getAllInstances(serviceId, group);
            for (Instance serviceInstance : instances) {
                if (serviceInstance.getIp().equalsIgnoreCase(RegisterContext.INSTANCE.getClientInfo().getIp())
                        && serviceInstance.getPort() == RegisterContext.INSTANCE.getClientInfo().getPort()) {
                    return serviceInstance.isEnabled() ? "UP" : "DOWN";
                }
            }
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "getInstanceStatus failed,serviceId={%s}",
                    serviceId), e);
        }
        return STATUS_UNKNOW;
    }

    /**
     * Obtain a collection of service instances based on the service ID
     *
     * @param serviceId Service ID
     * @return List of service instances
     */
    public List<NacosServiceInstance> getInstances(String serviceId) {
        try {
            return Optional.of(nacosServiceDiscovery.getInstances(serviceId))
                    .map(instances -> {
                        ServiceCache.setInstances(serviceId, instances);
                        return instances;
                    }).get();
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "getInstances failed from nacos，"
                    + "serviceId={%s}，isFailureToleranceEnabled={%s}", serviceId,
                    nacosRegisterConfig.isFailureToleranceEnabled()), e);
            if (nacosRegisterConfig.isFailureToleranceEnabled()) {
                return ServiceCache.getInstances(serviceId);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Get the service name
     *
     * @return A collection of service names
     */
    public List<String> getServices() {
        try {
            return Optional.of(nacosServiceDiscovery.getServices()).map(services -> {
                ServiceCache.setServiceIds(services);
                return services;
            }).get();
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "getServices failed，"
                    + "isFailureToleranceEnabled={%s}", nacosRegisterConfig.isFailureToleranceEnabled()), e);
            return nacosRegisterConfig.isFailureToleranceEnabled() ? ServiceCache.getServiceIds()
                    : Collections.emptyList();
        }
    }
}