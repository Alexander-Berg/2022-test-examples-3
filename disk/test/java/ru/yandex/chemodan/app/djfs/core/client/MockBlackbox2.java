package ru.yandex.chemodan.app.djfs.core.client;

import java.util.HashMap;
import java.util.List;

import lombok.Builder;
import lombok.Value;
import org.joda.time.Duration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxQueryable;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutor;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxException;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.request.BlackboxRequest;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.BlackboxAuthType;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.BlackboxSid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.EmailsParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.PhoneAttributesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAbstractResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAddressWithMailDB;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAttributes;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxBulkResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDbFields;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDisplayName;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.ip.IpAddress;
import ru.yandex.misc.lang.Validate;

/**
 * @author eoshch
 */
public class MockBlackbox2 extends Blackbox2 {
    private static final MapF<PassportUid, BlackboxUser> users = Cf.wrap(new HashMap<>());

    public MockBlackbox2() {
        super(new MockBlackboxRequestExecutor());
    }

    public void add(
            DjfsUid uid, String login, String firstname, String lastname, String lang, String publicName,
            boolean accountIsAvailable
    ) {
        add(
                uid,
                BlackboxUser.builder()
                        .uid(uid.asPassportUid())
                        .domain(PassportDomain.YANDEX_RU)
                        .login(login)
                        .firstname(firstname)
                        .lastname(lastname)
                        .lang(lang)
                        .publicName(publicName)
                        .accountIsAvailable(accountIsAvailable)
                        .build()
        );
    }

    public void add(DjfsUid uid, String login, String publicName) {
        add(uid, login, "Firstname", "Lastname", "RU", publicName, true);
    }

    public void add(DjfsUid uid, BlackboxUser user) {
        Validate.isFalse(users.containsKeyTs(uid.asPassportUid()));
        users.put(uid.asPassportUid(), user);
    }

    public void clear() {
        users.clear();
    }

    private static class MockBlackboxRequestExecutor extends BlackboxRequestExecutor {
        @Override
        public Either<BlackboxAbstractResponse, BlackboxBulkResponse> execute(BlackboxRequest request) {
            throw new NotImplementedException();
        }

        @Override
        public MockBlackboxQueryable asQueryable() {
            return new MockBlackboxQueryable();
        }
    }

    private static class MockBlackboxQueryable extends BlackboxQueryable {
        public MockBlackboxQueryable() {
            super(null);
        }

        @Override
        public BlackboxAbstractResponse login(IpAddress userIp, String login, String password, Option<BlackboxSid> sid,
                Option<Boolean> captcha, List<String> dbFields, List<Integer> attributes,
                Option<BlackboxAuthType> authType, boolean getUserTicket) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxAbstractResponse login(IpAddress userIp, String login, String password, Option<BlackboxSid> sid,
                Option<Boolean> captcha, List<String> dbFields, List<Integer> attributes,
                Option<BlackboxAuthType> authType) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxAbstractResponse login(IpAddress userIp, String login, String password, Option<BlackboxSid> sid,
                Option<Boolean> captcha, List<String> dbFields, Option<BlackboxAuthType> authType)
                throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse login(IpAddress userIp, String login, String password) throws BlackboxException {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse login(IpAddress userIp, String login, String password, BlackboxSid sid)
                throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse login(IpAddress userIp, String login, String password, List<String> dbFields)
                throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse login(IpAddress userIp, String login, String password, List<String> dbFields,
                List<Integer> attributes) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse login(IpAddress userIp, String login, String password, boolean captcha,
                List<String> dbFields) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse login(IpAddress userIp, String login, String password, BlackboxSid sid,
                boolean captcha, List<String> dbFields) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxAddressWithMailDB getEmailInfo(IpAddress ipAddress, PassportUid uid, Email email) {
            throw new NotImplementedException();
        }

        @Override
        public MapF<PassportUid, BlackboxAbstractResponse> userInfoBulk(IpAddress userIp, ListF<PassportUid> uids,
                List<String> dbFields, Option<EmailsParameterValue> emails, Option<AliasesParameterValue> aliases,
                boolean regName, Option<ListF<PhoneAttributesParameterValue>> phoneAttributes) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public MapF<PassportUid, BlackboxAbstractResponse> userInfoBulk(IpAddress userIp, ListF<PassportUid> uids,
                List<String> dbFields, Option<EmailsParameterValue> emails, Option<AliasesParameterValue> aliases,
                boolean regName) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse userInfo(IpAddress userIp, Option<PassportUid> uid, Option<String> login,
                Option<BlackboxSid> sid, List<String> dbFields, ListF<Integer> attributes,
                Option<EmailsParameterValue> emails,
                Option<AliasesParameterValue> aliases, boolean regName,
                Option<ListF<PhoneAttributesParameterValue>> phoneAttributes, boolean isDisplayNameEmpty,
                boolean getPublicName)
                throws BlackboxException
        {
            Option<BlackboxUser> userO = uid.filterMap(MockBlackbox2.users::getO);

            BlackboxResponseBuilder result = new BlackboxResponseBuilder(BlackboxMethod.USER_INFO);
            result.setStatus(-1);
            result.setLogin(userO.map(BlackboxUser::getLogin));
            result.setUidDomain(userO.map(x -> Tuple2.tuple(x.getUid(), x.getDomain())));

            if (userO.isPresent()) {
                BlackboxUser user = userO.get();
                MapF<String, String> map = Cf.wrap(new HashMap<>());
                for (String dbField : dbFields) {
                    if (BlackboxDbFields.FIRSTNAME.equals(dbField)) {
                        map.put(BlackboxDbFields.FIRSTNAME, user.firstname);
                    } else if (BlackboxDbFields.LASTNAME.equals(dbField)) {
                        map.put(BlackboxDbFields.LASTNAME, user.lastname);
                    } else if (BlackboxDbFields.LANG.equals(dbField)) {
                        map.put(BlackboxDbFields.LANG, user.lang);
                    } else if (BlackboxDbFields.COUNTRY.equals(dbField)) {
                        map.put(BlackboxDbFields.COUNTRY, user.country);
                    } else {
                        throw new NotImplementedException();
                    }
                }
                result.setDbFields(map);

                BlackboxDisplayName displayName = new BlackboxDisplayName(user.login,
                        Option.empty(), Option.empty(), Option.empty(), Option.when(getPublicName, user.publicName));

                result.setDisplayName(Option.of(displayName));
                result.setDefaultEmail(Option.of(new Email(user.login + "@" + user.domain)));
                MapF<Integer, String> userAttributes = Cf.hashMap();
                if (attributes.containsTs(BlackboxAttributes.ACCOUNT_IS_AVAILABLE)) {
                    userAttributes.put(BlackboxAttributes.ACCOUNT_IS_AVAILABLE, user.accountIsAvailable ?"1" : "0");
                }
                result.setAttributes(userAttributes);
            }

            return (BlackboxCorrectResponse) result.build();
        }

        @Override
        public MapF<PassportUid, BlackboxAbstractResponse> userInfoBulk(IpAddress userIp, ListF<PassportUid> uids,
                List<String> dbFields)
        {
            throw new NotImplementedException();
        }

        @Override
        public MapF<PassportUid, BlackboxAbstractResponse> userInfoBulk(IpAddress userIp, ListF<PassportUid> uids,
                List<String> dbFields, boolean regName)
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse sessionId(IpAddress userIp, String sessionId, String host, List<String> dbFields,
                boolean renew, Option<String> sslSessionId) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse sessionId(IpAddress userIp, String sessionId, String host, List<String> dbFields,
                boolean renew) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse sessionId(IpAddress userIp, String sessionId, String host)
                throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token, List<String> dbFields,
                List<Integer> attributes, Option<EmailsParameterValue> emails, Option<AliasesParameterValue> aliases,
                boolean getUserTicket) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token, List<String> dbFields,
                List<Integer> attributes, Option<EmailsParameterValue> emails, Option<AliasesParameterValue> aliases)
                throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token, List<String> dbFields,
                Option<EmailsParameterValue> emails, Option<AliasesParameterValue> aliases) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token, List<String> dbFields,
                Option<EmailsParameterValue> emails) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token, List<String> dbFields)
                throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token, List<String> dbFields,
                List<Integer> attributes) throws BlackboxException
        {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse oAuth(IpAddress userIp, String token) throws BlackboxException {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse sign(String toSign, Option<Duration> tokenLife) {
            throw new NotImplementedException();
        }

        @Override
        public BlackboxCorrectResponse sign(String toSign) {
            throw new NotImplementedException();
        }
    }

    @Value
    @Builder
    private static class BlackboxUser {
        PassportUid uid;
        PassportDomain domain;
        String login;
        String firstname;
        String lastname;
        String lang;
        String publicName;
        boolean accountIsAvailable;
        String country;
    }

    public static BlackboxUser.BlackboxUserBuilder userBuilder() {
        return BlackboxUser.builder();
    }
}
