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

package com.huaweicloud.sermant.tag.transmission.wrapper;

import com.huaweicloud.sermant.core.utils.tag.TrafficData;
import com.huaweicloud.sermant.core.utils.tag.TrafficTag;

import java.util.concurrent.Callable;

/**
 * Runnable&Callable包装类，例如reactor.core.scheduler.WorkerTask
 *
 * @param <T> 泛型
 * @author provenceee
 * @since 2023-04-21
 */
public class RunnableAndCallableWrapper<T> extends AbstractThreadWrapper<T> implements Runnable, Callable<T> {
    /**
     * 构造方法
     *
     * @param runnable runnable
     * @param callable callable
     * @param trafficTag 请求标记
     * @param trafficData 请求数据
     * @param cannotTransmit 执行方法之前是否需要删除线程变量
     */
    public RunnableAndCallableWrapper(Runnable runnable, Callable<T> callable, TrafficTag trafficTag,
            TrafficData trafficData, boolean cannotTransmit) {
        super(runnable, callable, trafficTag, trafficData, cannotTransmit);
    }
}