/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.itests.smoke;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ProfileRequirements;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class AutoClusterStartupTest extends FabricTestSupport {
    protected FabricService fabricService;

    @Test
    @Ignore("[FABRIC-521] Fix fabric/fabric-itests/fabric-itests-smoke")
    public void testLocalFabricCluster() throws Exception {
        fabricService = getFabricService();
        //Test autostartup.
        assertNotNull(fabricService);
        Thread.sleep(5000);
        CuratorFramework curator = getCurator();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        Provision.containerAlive(Arrays.<Container>asList(new ContainerImpl(null, "root", fabricService)), PROVISION_TIMEOUT);
        Container[] containers = fabricService.getContainers();
        assertNotNull(containers);
        assertEquals("Expected to find 1 container", 1, containers.length);
        assertEquals("Expected to find the root container", "root", containers[0].getId());

        //Test that a generated password exists
        //We don't inject the configuration admin as it causes issues when the tracker gets closed.
        ConfigurationAdmin configurationAdmin = getOsgiService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
        Dictionary<String, Object> dictionary = configuration.getProperties();
        assertNotNull("Expected a generated zookeeper password", dictionary.get("zookeeper.password"));
        assertTrue(String.valueOf(dictionary.get("zookeeper.url")).endsWith("2182"));

        assertAutoScaling();
    }

    protected void assertAutoScaling() throws Exception {
        String profile = "mq-amq";
        Integer expected = 1;
        boolean changed = fabricService.scaleProfile(profile, expected);
        assertProfileMinimumSize(profile, expected);

        // lets call the scale method again, should have no effect as already requirements are updated
        // and we've not started an auto-scaler yet
        changed = fabricService.scaleProfile(profile, expected);
        assertProfileMinimumSize(profile, expected);
        Assert.assertEquals("should not have changed!", false, changed);


        changed = fabricService.scaleProfile(profile, 2);
        assertProfileMinimumSize(profile, 2);

        // now lets scale down
        changed = fabricService.scaleProfile(profile, -1);

        // since we have no instances right now, scaling down just removes the minimumInstances requirements ;)
        assertProfileMinimumSize(profile, null);
    }

    protected void assertProfileMinimumSize(String profile, Integer expected) throws IOException {
        FabricRequirements requirements = fabricService.getRequirements();
        ProfileRequirements profileRequirements = requirements.getOrCreateProfileRequirement(profile);
        Assert.assertNotNull("Should have profile requirements for profile " + profile, profileRequirements);
        Assert.assertEquals("profile " + profile + " minimum instances", expected, profileRequirements.getMinimumInstances());
        System.out.println("Profile " + profile + " now has requirements " + profileRequirements);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                new VMOption("-D" + CreateEnsembleOptions.ENSEMBLE_AUTOSTART + "=true"),
                new VMOption("-D" + CreateEnsembleOptions.AGENT_AUTOSTART + "=false"),
                new VMOption("-D" + CreateEnsembleOptions.ZOOKEEPER_SERVER_PORT + "=2182"),
                new VMOption("-D" + CreateEnsembleOptions.ZOOKEEPER_SERVER_CONNECTION_PORT + "=2182")
        };
    }
}
