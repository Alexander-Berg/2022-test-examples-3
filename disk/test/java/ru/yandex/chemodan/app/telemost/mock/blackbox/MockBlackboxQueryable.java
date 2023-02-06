package ru.yandex.chemodan.app.telemost.mock.blackbox;

import java.util.List;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.BlackboxQueryable;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutor;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxException;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxHttpException;
import ru.yandex.inside.passport.blackbox2.protocol.request.BlackboxRequest;
import ru.yandex.inside.passport.blackbox2.protocol.request.BlackboxRequestBuilder;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.EmailsParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.PhoneAttributesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAbstractResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxBulkResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxErrorResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseParseException;
import ru.yandex.misc.ip.IpAddress;

import static ru.yandex.inside.passport.YandexAccounts.SERVICE_TICKET;

public class MockBlackboxQueryable extends BlackboxQueryable {

    public static final String IS_BULK_QUERY = "is_bulk";

    private final BlackboxRequestExecutor executor;

    public MockBlackboxQueryable(BlackboxRequestExecutor executor) {
        super(executor);
        this.executor = executor;
    }

    public MapF<PassportUid, BlackboxAbstractResponse> userInfoBulk(
            IpAddress userIp,
            ListF<PassportUid> uids,
            List<String> dbFields,
            ListF<Integer> attributes,
            Option<EmailsParameterValue> emails,
            Option<AliasesParameterValue> aliases,
            boolean regName,
            Option<ListF<PhoneAttributesParameterValue>> phoneAttributes,
            Option<String> tvmServiceTicket) throws BlackboxException
    {
        if (uids.size() == 1) {
            return Cf.map(
                    uids.first(),
                    userInfo(userIp, uids.firstO(), Option.empty(), Option.empty(), dbFields, attributes,
                            emails, aliases, regName, phoneAttributes, false, false, tvmServiceTicket));
        }

        BlackboxRequestBuilder builder = BlackboxRequest.query()
                .userInfo()
                .userIp(userIp);

        builder.uid(uids);

        if (dbFields != null && !dbFields.isEmpty()) {
            builder.dbFields(Cf.x(dbFields));
        }
        if (attributes != null && !attributes.isEmpty()) {
            builder.attributes(Cf.x(attributes));
        }
        if (emails.isPresent()) {
            builder.emails(emails.get());
        }
        if (aliases.isPresent()) {
            builder.aliases(aliases.get());
        }
        if (regName) {
            builder.regName("yes");
        }
        if (phoneAttributes.isPresent()) {
            builder.phoneAttributes(phoneAttributes.get());
        }
        if (tvmServiceTicket.isPresent()) {
            builder.addHeader(SERVICE_TICKET, tvmServiceTicket.get());
        }

        builder.addHeader(IS_BULK_QUERY, String.valueOf(true));

        Either<BlackboxAbstractResponse, BlackboxBulkResponse> response = executor.execute(builder.build());
        if (response.isLeft()) {
            BlackboxAbstractResponse leftResponse = response.getLeft();
            if (leftResponse instanceof BlackboxErrorResponse) {
                throw new BlackboxHttpException("Inner error in userInfoBulk",
                        ((BlackboxErrorResponse) leftResponse).exception());
            } else if (leftResponse instanceof BlackboxCorrectResponse) {
                throw new BlackboxResponseParseException("Expected error, but was single result in bulk method");
            } else {
                throw new BlackboxResponseParseException("Unknown BlackboxAbstractResponse subtype");
            }
        }
        return response.getRight().get();
    }
}
