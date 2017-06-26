package org.openbase.bco.authentication.lib;

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
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import rst.domotic.authentication.TicketType.Ticket;
import rst.timing.IntervalType.Interval;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class AuthenticationServerHandler {

    /**
     * 15 minutes in milli seconds.
     */
    public static long VALIDITY_PERIOD_IN_MILLIS = 15 * 60 * 1000;

    /**
     * Handles a Key Distribution Center (KDC) login request
     * Creates a Ticket Granting Server (TGS) session key that is encrypted by the client's password
     * Creates a Ticket Granting Ticket (TGT) that is encrypted by TGS private key
     *
     * @param clientID Identifier of the client - must be present in client database
     * @param clientPasswordHash the hashed password of the client
     * @param clientNetworkAddress Network address of client
     * @param ticketGrantingServiceSessionKey TGS session key generated by controller
     * @param ticketGrantingServicePrivateKey TGS private key generated by controller or saved somewhere in the system
     * @return Returns wrapper class containing both the TGT and TGS session key
     *
     * @throws NotAvailableException Throws, if clientID was not found in database
     * @throws CouldNotPerformException If the data for the remotes has not been synchronized yet.
     * @throws InterruptedException If the Registry thread is interrupted externally.
     * @throws IOException If an encryption operation fails because of a general I/O error.
     */
    public static TicketSessionKeyWrapper handleKDCRequest(final String clientID, final byte[] clientPasswordHash, final String clientNetworkAddress, final byte[] ticketGrantingServiceSessionKey, final byte[] ticketGrantingServicePrivateKey) throws NotAvailableException, InterruptedException, CouldNotPerformException, IOException {
        // create ticket granting ticket
        Ticket.Builder ticketGrantingTicket = Ticket.newBuilder();
        ticketGrantingTicket.setClientId(clientID);
        ticketGrantingTicket.setClientIp(clientNetworkAddress);
        ticketGrantingTicket.setValidityPeriod(getValidityInterval());
        ticketGrantingTicket.setSessionKeyBytes(ByteString.copyFrom(ticketGrantingServiceSessionKey));

        // create TicketSessionKeyWrapper
        TicketSessionKeyWrapper.Builder ticketSessionKeyWrapper = TicketSessionKeyWrapper.newBuilder();
        ticketSessionKeyWrapper.setTicket(EncryptionHelper.encrypt(ticketGrantingTicket.build(), ticketGrantingServicePrivateKey));
        ticketSessionKeyWrapper.setSessionKey(EncryptionHelper.encrypt(ticketGrantingServiceSessionKey, clientPasswordHash));

        return ticketSessionKeyWrapper.build();
    }

    /**
     * Handles a Ticket Granting Service (TGS) request
     * Creates a Service Server (SS) session key that is encrypted with the TGS session key
     * Creates a Client Server Ticket (CST) that is encrypted by SS private key
     *
     * @param ticketGrantingServiceSessionKeyMap TGS session key generated by controller
     * @param ticketGrantingServicePrivateKey TGS private key generated by controller or saved somewhere in the system
     * @param serviceServerSessionKeyMap SS session key generated by the controller
     * @param serviceServerPrivateKey TGS private key generated by controller or saved somewhere in the system
     * @param wrapper TicketAuthenticatorWrapperWrapper that contains both encrypted Authenticator and TGT
     * @return Returns a wrapper class containing both the CST and SS session key
     *
     * @throws RejectedException If timestamp in Authenticator does not fit to time period in TGT
     * or, if clientID in Authenticator does not match clientID in TGT
     * @throws StreamCorruptedException If the decryption of the Authenticator or TGT fails, probably because the wrong keys were used.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static TicketSessionKeyWrapper handleTGSRequest(final Map<String, byte[]> ticketGrantingServiceSessionKeyMap, final Map<String, byte[]> serviceServerSessionKeyMap, final byte[] ticketGrantingServicePrivateKey, final byte[] serviceServerPrivateKey, final TicketAuthenticatorWrapper wrapper) throws RejectedException, StreamCorruptedException, IOException {
        // decrypt ticket and authenticator
        Ticket ticketGrantingTicket = (Ticket) EncryptionHelper.decrypt(wrapper.getTicket(), ticketGrantingServicePrivateKey);
        byte[] ticketGrantingServiceSessionKey = ticketGrantingServiceSessionKeyMap.get(ticketGrantingTicket.getClientId());
        Authenticator authenticator = (Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), ticketGrantingServiceSessionKey);

        // compare clientIDs and timestamp to period
        AuthenticationServerHandler.validateTicket(ticketGrantingTicket, authenticator);

        // generate new session key
        byte[] serviceServerSessionKey = EncryptionHelper.generateKey();
        serviceServerSessionKeyMap.put(authenticator.getClientId(), serviceServerSessionKey);

        // update period and session key
        Ticket.Builder clientServerTicket = ticketGrantingTicket.toBuilder();
        clientServerTicket.setValidityPeriod(getValidityInterval());
        clientServerTicket.setSessionKeyBytes(ByteString.copyFrom(serviceServerSessionKey));

        // create TicketSessionKeyWrapper
        TicketSessionKeyWrapper.Builder ticketSessionKeyWrapper = TicketSessionKeyWrapper.newBuilder();
        ticketSessionKeyWrapper.setTicket(EncryptionHelper.encrypt(clientServerTicket.build(), serviceServerPrivateKey));
        ticketSessionKeyWrapper.setSessionKey(EncryptionHelper.encrypt(serviceServerSessionKey, ticketGrantingServiceSessionKeyMap.get(authenticator.getClientId())));

        return ticketSessionKeyWrapper.build();
    }

    /**
     * Handles a service method (Remote) request to Service Server (SS) (Manager)
     * Updates given CST's validity period and encrypt again by SS private key
     *
     * @param serviceServerSesssionKeyMap SS session key generated by server
     * @param serviceServerPrivateKey SS private key only known to SS
     * @param wrapper TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and TGT
     * @return Returns a wrapper class containing both the modified CST and unchanged Authenticator
     *
     * @throws RejectedException If timestamp in Authenticator does not fit to time period in TGT
     * or, if clientID in Authenticator does not match clientID in TGT
     * @throws StreamCorruptedException If the decryption of the Authenticator or CST fails, probably because the wrong keys were used.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static TicketAuthenticatorWrapper handleSSRequest(final Map<String, byte[]> serviceServerSesssionKeyMap, final byte[] serviceServerPrivateKey, final TicketAuthenticatorWrapper wrapper) throws RejectedException, StreamCorruptedException, IOException {
        // decrypt ticket and authenticator
        Ticket clientServerTicket = (Ticket) EncryptionHelper.decrypt(wrapper.getTicket(), serviceServerPrivateKey);
        Authenticator authenticator = (Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), serviceServerSesssionKeyMap.get(clientServerTicket.getClientId()));

        // compare clientIDs and timestamp to period
        AuthenticationServerHandler.validateTicket(clientServerTicket, authenticator);

        // update period and session key
        Ticket.Builder cstb = clientServerTicket.toBuilder();
        cstb.setValidityPeriod(getValidityInterval());

        // update TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = wrapper.toBuilder();
        ticketAuthenticatorWrapper.setTicket(EncryptionHelper.encrypt(clientServerTicket, serviceServerPrivateKey));

        return ticketAuthenticatorWrapper.build();
    }

    private static void validateTicket(Ticket ticket, Authenticator authenticator) throws RejectedException {
        if (ticket.getClientId() == null) {
            throw new RejectedException("ClientId null in ticket");
        }
        if (authenticator.getClientId() == null) {
            throw new RejectedException("ClientId null in authenticator");
        }
        if (!authenticator.getClientId().equals(ticket.getClientId())) {
            throw new RejectedException("ClientIds do not match");
        }
        if (!AuthenticationServerHandler.isTimestampInInterval(authenticator.getTimestamp(), ticket.getValidityPeriod())) {
            throw new RejectedException("Session expired");
        }
    }

    /**
     * Test if the timestamp lies in the interval
     *
     * @param timestamp the timestamp checked
     * @param interval the interval checked
     * @return true if the timestamp is greater equals the start and lower equals the end of the interval
     */
    public static boolean isTimestampInInterval(final Timestamp timestamp, final Interval interval) {
        System.out.println("Timestamp ["+timestamp.getTime()+"] in Interval["+interval.getBegin().getTime()+", "+interval.getEnd().getTime()+"]");
        boolean val = timestamp.getTime() >= interval.getBegin().getTime() && timestamp.getTime() <= interval.getEnd().getTime();
        System.out.println("In interval " + val);
        return val;
//        return true;
    }

    /**
     * Generate an interval which begins now and has an end times 15 minutes from now.
     *
     * @return the above described interval
     */
    public static Interval getValidityInterval() {
        long currentTime = System.currentTimeMillis();
        Interval.Builder validityInterval = Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + VALIDITY_PERIOD_IN_MILLIS));
        return validityInterval.build();
    }
}
