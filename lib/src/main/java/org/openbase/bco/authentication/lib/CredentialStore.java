package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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

import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFileProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.LoginCredentialsCollectionType.LoginCredentialsCollection;
import rst.domotic.authentication.LoginCredentialsType.LoginCredentials;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

/**
 * This class provides access to the storage of login credentials.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin
 * Romankiewicz</a>
 */
public class CredentialStore {

    public static final String SERVICE_SERVER_ID = "serviceServer";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CredentialStore.class);
    protected final HashMap<String, LoginCredentials> credentials;
    private final String filename;
    private final File credentialFile;
    private final Base64.Encoder encoder;
    private final Base64.Decoder decoder;

    private final ProtoBufFileProcessor<LoginCredentialsCollection, LoginCredentialsCollection, LoginCredentialsCollection.Builder> fileProcessor;

    public CredentialStore(final String filename) throws InitializationException {
        try {
            this.filename = filename;
            this.credentialFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), filename);
            this.credentials = new HashMap<>();
            this.fileProcessor = new ProtoBufFileProcessor<>(LoginCredentialsCollection.newBuilder());
            this.encoder = Base64.getEncoder();
            this.decoder = Base64.getDecoder();
        } catch (JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            this.loadStore();
            this.setStorePermissions();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(CredentialStore.class, ex);
        }
    }

    /**
     * Loads the credentials from a protobuf JSON file.
     *
     * @throws CouldNotPerformException If the deserialization fails.
     */
    private void loadStore() throws CouldNotPerformException {
        // create empty store if not available
        if (!credentialFile.exists()) {
            saveStore();
        }

        // clear existing entries.
        credentials.clear();

        // load new ones out of the credential store.
        LoginCredentialsCollection collection = fileProcessor.deserialize(credentialFile);
        collection.getElementList().forEach((entry) -> {
            credentials.put(entry.getId(), entry);
        });
    }

    /**
     * Stores the credentials in a protobuf JSON file.
     */
    protected void saveStore() {
        try {
            // extract credentials
            LoginCredentialsCollection collection = LoginCredentialsCollection.newBuilder()
                    .addAllElement(credentials.values())
                    .build();

            // save into store
            fileProcessor.serialize(collection, credentialFile);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
        }
    }


    private void setStorePermissions() throws CouldNotPerformException {
        protectFile(credentialFile);
    }

    /**
     * Sets the permissions to UNIX 600 so only the owner has permission to read and to write to this protected file.
     *
     * @throws CouldNotPerformException is thrown if the file could not be protected.
     */
    public static void protectFile(final File file) throws CouldNotPerformException {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            try {
                Files.setPosixFilePermissions(file.toPath(), perms);
            } catch (UnsupportedOperationException ex) {
                // apply windows fallback
                System.out.println("Apply windows permission fallback for "+ file.getAbsolutePath());
                file.setReadable(true, true);
                file.setWritable(true, true);
                file.setExecutable(true, true);
            }
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not protect "+file.getAbsolutePath(), ex);
        }
    }

    /**
     * Return whether the internal map is empty.
     *
     * @return true if no client/user is registered
     */
    public boolean isEmpty() {
        return this.credentials.isEmpty();
    }

    /**
     * Return whether the internal map only holds one entry for the service server.
     *
     * @return true if no client/user is registered except the service server.
     */
    public boolean hasOnlyServiceServer() {
        return (this.credentials.size() == 1 && this.credentials.containsKey(SERVICE_SERVER_ID));
    }

    /**
     * --------------------- MANIPULATIVE METHODS ------------------------------
     */
    /**
     * Determines if there is an entry with given id.
     *
     * @param id the id to check
     *
     * @return true if existent, false otherwise
     */
    public boolean hasEntry(String id) {
        return this.credentials.containsKey(id);
    }

    /**
     * Get an entry by the specified index.
     * Usage only makes sense when there is only one entry in the store.
     * Else an arbitrary entry would be returned.
     *
     * @return Returns null if the index would result in a NullPointerException.
     */
    public SimpleEntry<String, LoginCredentials> getFirstEntry() {
        if (!this.credentials.isEmpty()) {
            String firstKey = (String) new ArrayList(this.credentials.keySet()).get(0);
            return new SimpleEntry(firstKey, this.credentials.get(firstKey));
        }
        return null;
    }

    /**
     * Removes entry from store given id.
     *
     * @param id the credentials to remove
     */
    public void removeEntry(String id) {
        if (this.hasEntry(id)) {
            this.credentials.remove(id);
        }
        this.saveStore();
    }

    /**
     * Get the encrypted login credentials for a given user.
     *
     * @param userId ID of the user whose credentials should be retrieved.
     *
     * @return The encrypted credentials, if they could be found.
     *
     * @throws NotAvailableException If the user does not exist in the
     *                               credentials storage.
     */
    public byte[] getCredentials(String userId) throws NotAvailableException {
        if (!credentials.containsKey(userId)) {
            throw new NotAvailableException(userId);
        }
        return decoder.decode(credentials.get(userId).getCredentials());
    }

    /**
     * Sets the login credentials for a given user. If there is already an entry
     * in the storage for this user, it will be replaced. Otherwise, a new entry
     * will be created.
     *
     * @param userId      ID of the user to modify.
     * @param credentials New encrypted credentials.
     */
    public void setCredentials(String userId, byte[] credentials) {
        if (!this.credentials.containsKey(userId)) {
            this.addCredentials(userId, credentials, false);
        } else {
            this.addCredentials(userId, credentials, this.credentials.get(userId).getAdmin());
        }
    }

    /**
     * Adds new credentials to the store.
     *
     * @param id          id of client or user
     * @param credentials password, public or private key
     * @param admin       admin flag
     */
    public void addCredentials(String id, byte[] credentials, boolean admin) {
        LoginCredentials loginCredentials = LoginCredentials.newBuilder()
                .setId(id)
                .setCredentials(encoder.encodeToString(credentials))
                .setAdmin(admin)
                .build();

        this.credentials.put(id, loginCredentials);
        this.saveStore();
    }

    /**
     * Tells whether a given user has administrator permissions.
     *
     * @param userId ID of the user whose credentials should be retrieved.
     *
     * @return Boolean value indicating whether the user has administrator
     * permissions.
     *
     * @throws NotAvailableException If the user does not exist in the
     *                               credentials storage.
     */
    public boolean isAdmin(String userId) throws NotAvailableException {
        if (!credentials.containsKey(userId)) {
            return false;
        }

        return credentials.get(userId).getAdmin();
    }

    /**
     * Changes the admin flag of an entry.
     *
     * @param userId  user to change flag of
     * @param isAdmin boolean whether user is admin or not
     *
     * @throws NotAvailableException Throws if there is no user given userId
     */
    public void setAdmin(String userId, boolean isAdmin) throws NotAvailableException {
        if (!credentials.containsKey(userId)) {
            throw new NotAvailableException(userId);
        }
        LoginCredentials loginCredentials = LoginCredentials.newBuilder(this.credentials.get(userId))
                .setAdmin(isAdmin)
                .build();
        this.credentials.put(userId, loginCredentials);
        this.saveStore();
    }

    public void shutdown() {
        if (JPService.testMode()) {
            credentials.clear();
        }
        saveStore();
    }
}
