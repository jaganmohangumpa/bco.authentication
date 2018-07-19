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

import rst.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 * Class containing information an authenticated server may need to perform an authenticated request.
 * This data type is created after authentication is performed.
 * It contains the id of the user authenticated, the session key, the updated ticket which should be send back to the client.
 * It may contain an authentication and authorization token if they were send with the request.
 * <p>
 * This data should be used for authorization and to perform more authenticated requests in the users name.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthenticationBaseData {

    private AuthenticationToken authenticationToken;
    private AuthorizationToken authorizationToken;
    private final String userId;
    private final byte[] sessionKey;
    private final TicketAuthenticatorWrapper ticketAuthenticatorWrapper;

    /**
     * Create new authentication base data.
     *
     * @param userId                     the id of the authenticated user
     * @param sessionKey                 the session key
     * @param ticketAuthenticatorWrapper the updated ticket which should be send as a response to the client
     * @param authenticationToken        the authentication token send with the request
     * @param authorizationToken         the authorization token send with the request.
     */
    public AuthenticationBaseData(
            final String userId,
            final byte[] sessionKey,
            final TicketAuthenticatorWrapper ticketAuthenticatorWrapper,
            final AuthenticationToken authenticationToken,
            final AuthorizationToken authorizationToken) {
        this.userId = userId;
        this.sessionKey = sessionKey;
        this.ticketAuthenticatorWrapper = ticketAuthenticatorWrapper;
        this.authenticationToken = authenticationToken;
        this.authorizationToken = authorizationToken;
    }

    /**
     * Create new authentication base data.
     *
     * @param userId                     the id of the authenticated user
     * @param sessionKey                 the session key
     * @param ticketAuthenticatorWrapper the updated ticket which should be send as a response to the client
     */
    public AuthenticationBaseData(
            final String userId,
            final byte[] sessionKey,
            final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        this(userId, sessionKey, ticketAuthenticatorWrapper, null, null);
    }

    public String getUserId() {
        return userId;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public TicketAuthenticatorWrapper getTicketAuthenticatorWrapper() {
        return ticketAuthenticatorWrapper;
    }

    public AuthenticationToken getAuthenticationToken() {
        return authenticationToken;
    }

    public AuthorizationToken getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthenticationToken(AuthenticationToken authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public void setAuthorizationToken(AuthorizationToken authorizationToken) {
        this.authorizationToken = authorizationToken;
    }
}
