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

import org.openbase.jul.pattern.Pair;

/**
 * A pair of authentication and authorization user.
 */
public class AuthPair {

    /**
     * The id of the user who authenticate an action.
     */
    private final String authenticatedBy;

    /**
     * The id of the user who authorize an action.
     */
    private final String authorizedBy;

    /**
     * Creates a new empty auth pair which means the action is performed with other rights.
     */
    public AuthPair() {
        this.authenticatedBy = null;
        this.authorizedBy = null;
    }

    /**
     * Creates a new auth user pair.
     *
     * @param authenticatedUser the id of the user@client who is authenticated
     */
    public AuthPair(final String authenticatedUser) {
        final Pair<String, String> userIdPair = resolveUserIdPair(authenticatedUser);
        this.authenticatedBy = (userIdPair.getKey() != null) ? userIdPair.getKey() : userIdPair.getValue();
        this.authorizedBy = null;
    }

    /**
     * Creates a new auth user pair.
     *
     * @param authenticatedBy the id of the user who authenticate an action.
     * @param authorizedBy    the id of the user who authorize an action.
     */
    public AuthPair(String authenticatedBy, String authorizedBy) {
        this.authenticatedBy = resolveAuthUser(resolveUserIdPair(authenticatedBy));
        this.authorizedBy = resolveAuthUser(resolveUserIdPair(authorizedBy));
    }

    /**
     * The id of the user who authenticate an action.
     *
     * @return the user unit id as string.
     */
    public String getAuthenticatedBy() {
        return authenticatedBy;
    }

    /**
     * The id of the user who authorize an action.
     *
     * @return the user unit id as string.
     */
    public String getAuthorizedBy() {
        return authorizedBy;
    }

    /**
     * Resolves the auth by user id.
     *
     * @param pair the user - client string.
     *
     * @return returns the user if available, otherwise the client id.
     */
    private static String resolveAuthUser(Pair<String, String> pair) {
        if (pair.getKey() != null) {
            return pair.getKey();
        }
        return pair.getValue();
    }

    /**
     * Method resolves a user string.
     *
     * @param userClientString the user - client string e.g. user@client
     *
     * @return a pair where the key is the user and the value the client entry.
     */
    private static Pair<String, String> resolveUserIdPair(final String userClientString) {
        String user = null;
        String client = null;

        if (userClientString != null && !userClientString.isEmpty()) {
            final String[] split = userClientString.split("@");
            if (split.length > 1) {
                if (!split[0].isEmpty()) {
                    user = split[0];
                }
                if (!split[1].isEmpty()) {
                    client = split[1];
                }
            } else {
                client = userClientString.replace("@", "");
            }
        }
        return new Pair<>(user, client);
    }
}
