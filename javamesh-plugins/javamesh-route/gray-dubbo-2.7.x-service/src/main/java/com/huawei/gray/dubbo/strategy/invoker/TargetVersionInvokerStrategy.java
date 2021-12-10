/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved
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

package com.huawei.gray.dubbo.strategy.invoker;

import com.huawei.gray.dubbo.strategy.InvokerStrategy;
import com.huawei.gray.dubbo.strategy.VersionStrategy;

import org.apache.dubbo.rpc.Invoker;

import java.util.Set;

/**
 * 匹配targetVersion中的Invoker
 *
 * @author pengyuyi
 * @date 2021/12/8
 */
public class TargetVersionInvokerStrategy implements InvokerStrategy {
    /**
     * 匹配targetVersion中的Invoker
     *
     * @param invoker Invoker
     * @param version 目标版本
     * @param notMatchVersions 没有匹配上的版本
     * @param versionStrategy 版本测试
     * @return 是否匹配
     */
    @Override
    public boolean isMatch(Invoker<?> invoker, String version, Set<String> notMatchVersions,
            VersionStrategy versionStrategy) {
        return version.equals(versionStrategy.getVersion(invoker));
    }
}