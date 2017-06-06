package org.openbase.bco.authenticator.lib;

import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentification.AuthenticatorTicketType;
import rst.domotic.authentification.AuthenticatorType;
import rst.domotic.authentification.LoginResponseType;
import rst.timing.TimestampType;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class AuthenticationClientHandlerImpl implements AuthenticationClientHandler {
    
    @Override
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, LoginResponseType.LoginResponse wrapper) throws RejectedException {
        // decrypt TGS session key
        byte[] TGSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), hashedClientPassword);

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        AuthenticatorType.Authenticator.Builder ab = AuthenticatorType.Authenticator.newBuilder();
        ab.setClientId(clientID);
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        AuthenticatorTicketType.AuthenticatorTicket.Builder atb = AuthenticatorTicketType.AuthenticatorTicket.newBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), TGSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(atb.build());
        list.add(TGSSessionKey);

        return list;
    }

    @Override
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, LoginResponseType.LoginResponse wrapper) throws RejectedException {
        // decrypt SS session key
        byte[] SSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), TGSSessionKey);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        AuthenticatorType.Authenticator.Builder ab = AuthenticatorType.Authenticator.newBuilder();
        ab.setClientId(clientID);

        // create TicketAuthenticatorWrapper
        AuthenticatorTicketType.AuthenticatorTicket.Builder atb = AuthenticatorTicketType.AuthenticatorTicket.newBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), SSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<Object>();
        list.add(atb.build());
        list.add(SSSessionKey);

        return list;
    }

    @Override
    public AuthenticatorTicketType.AuthenticatorTicket initSSRequest(byte[] SSSessionKey, AuthenticatorTicketType.AuthenticatorTicket wrapper) throws RejectedException {
        // decrypt authenticator
        AuthenticatorType.Authenticator authenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), SSSessionKey);

        // create Authenticator
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        AuthenticatorType.Authenticator.Builder ab = authenticator.toBuilder();
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        AuthenticatorTicketType.AuthenticatorTicket.Builder atb = wrapper.toBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), SSSessionKey));

        return atb.build();
    }

    @Override
    public AuthenticatorTicketType.AuthenticatorTicket handleSSResponse(byte[] SSSessionKey, AuthenticatorTicketType.AuthenticatorTicket lastWrapper, AuthenticatorTicketType.AuthenticatorTicket currentWrapper) throws RejectedException {
        // decrypt authenticators
        AuthenticatorType.Authenticator lastAuthenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(lastWrapper.getAuthenticator(), SSSessionKey);
        AuthenticatorType.Authenticator currentAuthenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(currentWrapper.getAuthenticator(), SSSessionKey);

        // compare both timestamps
        
        this.validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    private void validateTimestamp(TimestampType.Timestamp now, TimestampType.Timestamp then) throws RejectedException {
        if (now.getTime() != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }

}
