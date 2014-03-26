/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.ha.lock;

import java.net.URI;

import org.neo4j.helpers.Factory;
import org.neo4j.kernel.AvailabilityGuard;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.ha.DelegateInvocationHandler;
import org.neo4j.kernel.ha.HaSettings;
import org.neo4j.kernel.ha.HaXaDataSourceManager;
import org.neo4j.kernel.ha.cluster.AbstractModeSwitcher;
import org.neo4j.kernel.ha.cluster.HighAvailabilityMemberStateMachine;
import org.neo4j.kernel.ha.com.RequestContextFactory;
import org.neo4j.kernel.ha.com.master.Master;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.transaction.AbstractTransactionManager;
import org.neo4j.kernel.impl.transaction.RemoteTxHook;

public class LockManagerModeSwitcher extends AbstractModeSwitcher<Locks>
{
    private final HaXaDataSourceManager xaDsm;
    private final DelegateInvocationHandler<Master> master;
    private final RequestContextFactory requestContextFactory;
    private final AbstractTransactionManager txManager;
    private final RemoteTxHook remoteTxHook;
    private final AvailabilityGuard availabilityGuard;
    private final Config config;
    private final Factory<Locks> locksFactory;

    public LockManagerModeSwitcher( HighAvailabilityMemberStateMachine stateMachine,
                                    DelegateInvocationHandler<Locks> delegate,
                                    HaXaDataSourceManager xaDsm, DelegateInvocationHandler<Master> master,
                                    RequestContextFactory requestContextFactory, AbstractTransactionManager txManager,
                                    RemoteTxHook remoteTxHook, AvailabilityGuard availabilityGuard, Config config,
                                    Factory<Locks> locksFactory)
    {
        super( stateMachine, delegate );
        this.xaDsm = xaDsm;
        this.master = master;
        this.requestContextFactory = requestContextFactory;
        this.txManager = txManager;
        this.remoteTxHook = remoteTxHook;
        this.availabilityGuard = availabilityGuard;
        this.config = config;
        this.locksFactory = locksFactory;
    }

    @Override
    protected Locks getMasterImpl()
    {
        return locksFactory.newInstance();
    }

    @Override
    protected Locks getSlaveImpl( URI serverHaUri )
    {
        return new SlaveLockManager( locksFactory.newInstance(), requestContextFactory, master.cement(), xaDsm, txManager,
                remoteTxHook, availabilityGuard, new SlaveLockManager.Configuration()
        {
            @Override
            public long getAvailabilityTimeout()
            {
                return config.get( HaSettings.lock_read_timeout );
            }
        });
    }
}
