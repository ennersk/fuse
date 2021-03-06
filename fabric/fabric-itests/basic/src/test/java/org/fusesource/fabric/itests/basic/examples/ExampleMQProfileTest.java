/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.itests.basic.examples;


import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import scala.actors.threadpool.Arrays;

import java.util.Set;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleMQProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    @Ignore("[FABRIC-590] Fix fabric/fabric-itests/fabric-itests-basic")
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        CuratorFramework curator = getCurator();
        Set<Container> containers = ContainerBuilder.create(2).withName("cnt").withProfiles("default").assertProvisioningResult().build();
        Container broker = containers.iterator().next();
        containers.remove(broker);

        setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(broker.getId()), "changing");
        System.err.println(executeCommand("fabric:container-change-profile " + broker.getId() + " mq-default"));
        Provision.provisioningSuccess(Arrays.asList(new Container[]{broker}), PROVISION_TIMEOUT);
        System.err.println(executeCommand("fabric:cluster-list"));

        for(Container c : containers) {
            setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing");
            System.err.println(executeCommand("fabric:container-change-profile " + c.getId() + " example-mq"));
        }
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);

        System.err.println(executeCommand("fabric:cluster-list"));
        System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+broker.getId()+" activemq:bstat"));
        String output = executeCommand("fabric:container-connect -u admin -p admin "+broker.getId()+" activemq:query -QQueue=FABRIC.DEMO");
        Assert.assertTrue(output.contains("DequeueCount = ") && !output.contains("DequeueCount = 0"));
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
