package org.openbase.bco.authentication.lib;

import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/*-
 * #%L
 * BCO Authentication Library
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
/**
 * Interface defining a service for Kerberos authentication.
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public interface AuthenticationService {

    /**
     * Request a TicketGrantingTicket from the AuthenticatorService. The reply
     * is a TicketSessionKeyWrapper that contains the TicketGrantingTicket
     * encrypted with the private key of the TicketGrantingService and the
     * session key for the TicketGrantingService encrypted with the client
     * password.
     *
     * Afterwards the client has to decrypt the session key with his password
     * and create an authenticator encrypted with it. Then the unchanged
     * TicketGrantingTicket and the encrypted Authenticator form a
     * TicketAuthenticatorWrapper which is used to request a ClientServerTicket.
     *
     * @param clientId the id of the client whose password is used for the
     * encryption of the session key
     * @return the described TicketSessionKeyWrapper
     * @throws CouldNotPerformException if the request fails
     */
    @RPCMethod
    public Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(String clientId) throws CouldNotPerformException;

    /**
     * Request a ClientServerTicket from the AuthenticatorService. The reply is
     * a TicketSessionKeyWrapper that contains the ClientServerTicket encrypted
     * with the private key of the ServiceServer and the session key encrypted
     * with the TicketGrantingService session key that the client received when
     * requesting the TicketGrantingTicket.
     *
     * Afterwards the client has to decrypt the session key with the
     * TicketGrantingTicket session key and create an authenticator encrypted
     * with it. Then the unchanged ClientServerTicket and the encrypted
     * Authenticator form a TicketAuthenticatorWrapper which send to validate
     * the client every time he wants to perform an action.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     * encrypted with the TicketGrantingService session key and the unchanged
     * TicketGrantingTicket
     * @return a wrapper containing a ClientServerTicket and a session key as
     * described above
     * @throws CouldNotPerformException it the request fails
     */
    @RPCMethod
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException;

    /**
     * Validate a ClientServierTicket. If validation is successful the reply is
     * a TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     * encrypted with the session key and the unchanged ClientServerTicket
     * @return a TicketAuthenticatorWrapper as described above
     * @throws CouldNotPerformException if validation fails
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException;
}
