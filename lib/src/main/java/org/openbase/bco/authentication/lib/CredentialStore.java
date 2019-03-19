package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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

import com.google.protobuf.ByteString;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFileProcessor;
import org.openbase.type.domotic.authentication.LoginCredentialsCollectionType.LoginCredentialsCollection;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;

import java.util.Map;

/**
 * This class provides access to the storage of login credentials.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class CredentialStore extends AbstractProtectedStore<LoginCredentials, LoginCredentialsCollection> {

    /**
     * Id for the credentials for the service server.
     */
    public static final String SERVICE_SERVER_ID = "serviceServer";

    public CredentialStore() {
        super(new ProtoBufFileProcessor<>(LoginCredentialsCollection.newBuilder()));
    }

    /**
     * Get the encrypted login credentials for a given user.
     *
     * @param userId id of the user.
     *
     * @return the credentials of the user.
     *
     * @throws NotAvailableException if not credentials for the user are saved in the store.
     */
    public LoginCredentials getCredentials(final String userId) throws NotAvailableException {
        return getEntry(userId);
    }

    /**
     * Adds or replaces credentials for a user.
     *
     * @param userId      id of client or user
     * @param credentials password, public or private key
     * @param admin       admin flag
     * @param symmetric   flag defining if symmetric or asymmetric encryption should be used.
     */
    public void addCredentials(final String userId, final byte[] credentials, final boolean admin, final boolean symmetric) {
        final LoginCredentials loginCredentials = LoginCredentials.newBuilder()
                .setId(userId)
                .setCredentials(ByteString.copyFrom(credentials))
                .setAdmin(admin)
                .setSymmetric(symmetric)
                .build();
        addEntry(userId, loginCredentials);
    }

    /**
     * Tells whether a given user has administrator permissions.
     *
     * @param userId id of the user checked
     *
     * @return if the user with the possesses admin permissions, this is also false if not user with the given id exists
     */
    public boolean isAdmin(final String userId) {
        try {
            return getEntry(userId).getAdmin();
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    /**
     * Changes the admin flag of an entry.
     *
     * @param userId  user to change flag of
     * @param isAdmin boolean whether user is admin or not
     *
     * @throws NotAvailableException if there is no user given userId
     */
    public void setAdmin(final String userId, final boolean isAdmin) throws NotAvailableException {
        if (!hasEntry(userId)) {
            throw new NotAvailableException(userId);
        }
        addEntry(userId, getEntry(userId).toBuilder().setAdmin(isAdmin).build());
    }

    /**
     * {@inheritDoc}
     *
     * @param data        {@inheritDoc}
     * @param internalMap {@inheritDoc}
     */
    @Override
    protected void load(final LoginCredentialsCollection data, final Map<String, LoginCredentials> internalMap) {
        data.getLoginCredentialsList().forEach((entry) -> internalMap.put(entry.getId(), entry));
    }

    /**
     * {@inheritDoc}
     *
     * @param internalMap the internal map {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected LoginCredentialsCollection save(final Map<String, LoginCredentials> internalMap) {
        return LoginCredentialsCollection.newBuilder().addAllLoginCredentials(internalMap.values()).build();
    }

    /**
     * Query how many admin credentials are currently stored.
     *
     * @return the total number of admin credentials stored.
     */
    public int getAdminCount() {
        int adminCount = 0;
        for (LoginCredentials value : getEntryMap().values()) {
            if (value.getAdmin()) {
                adminCount++;
            }
        }
        return adminCount;
    }
}
