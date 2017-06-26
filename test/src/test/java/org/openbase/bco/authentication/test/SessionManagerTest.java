package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jps.core.JPService;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBListener;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.Event;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class SessionManagerTest {

    private static AuthenticatorLauncher authenticatorLauncher;
    private RSBListener listener;
    
    public SessionManagerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        MockRegistryHolder.newMockRegistry();
        
        GlobalCachedExecutorService.submit(() -> {
            authenticatorLauncher = new AuthenticatorLauncher();
            authenticatorLauncher.launch();
            return null;
        });
    }
    
    @AfterClass
    public static void tearDownClass() {
        if (authenticatorLauncher != null) {
            authenticatorLauncher.shutdown();
        }
        MockRegistryHolder.shutdownMockRegistry();
    }
    
    @Before
    public void setUp() throws Exception {
        listener = RSBFactoryImpl.getInstance().createSynchronizedListener(JPService.getProperty(JPAuthenticationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());
        listener.addHandler((Event event) -> {
//            System.out.println(event.getData());
        }, true);
        listener.activate();
    }
    
    @After
    public void tearDown() throws Exception {
        listener.deactivate();
    }

    /**
     * Test of login method, of class SessionManager.
     */
    @Test(timeout = 5000)
    public void testLogin() throws Exception {
        Registries.getUserRegistry().waitForData();
        UnitConfigType.UnitConfig userUnitConfig = Registries.getUserRegistry().getUserConfigs().get(0);
        String clientId = userUnitConfig.getId();
                
        SessionManager manager = new SessionManager();
        boolean result = manager.login(clientId, userUnitConfig.getUserConfig().getPassword().toString());
        
        assertEquals(true, result);
    }

    /**
     * Test of login method, of class SessionManager.
     */
    @Test(timeout = 5000)
    public void testLogout() throws Exception {
        Registries.getUserRegistry().waitForData();
        UnitConfigType.UnitConfig userUnitConfig = Registries.getUserRegistry().getUserConfigs().get(0);
        String clientId = userUnitConfig.getId();
                
        SessionManager manager = new SessionManager();
        boolean result = manager.login(clientId, userUnitConfig.getUserConfig().getPassword().toString());
        
        assertEquals(true, result);
        
        manager.logout();
        assertEquals(null, manager.getTicketAuthenticatorWrapper());
        assertEquals(null, manager.getSessionKey());
      
    }
    
}
