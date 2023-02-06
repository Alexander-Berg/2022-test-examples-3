package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.EmailOwnership;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public class EmailOwnershipMapperTest {

    private EmailOwnershipMapper mapper = new EmailOwnershipMapper();

    @Test
    public void testParseWithPuid() {
        String line = "tskv\t" +
                "ID_TYPE=UID\t" +
                "ID_VALUE=33478676\t" +
                "EMAIL=apershukov@yandex.ru\t" +
                "STATUS=CONFIRMED\t" +
                "SOURCE=PASSPORT\t" +
                "ACTIVE=true\t" +
                "MODIFICATION_TIME=12345\n";

        Collection<EmailOwnership> facts = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, facts.size());

        EmailOwnership expected = EmailOwnership.newBuilder()
                .setUid(
                        Uids.create(UidType.PUID, 33478676)
                )
                .setEmail("apershukov@yandex.ru")
                .setStatus("CONFIRMED")
                .setSource("PASSPORT")
                .setActive(true)
                .setModificationTime(12345)
                .build();

        assertEquals(expected, facts.iterator().next());
    }

    @Test
    public void testParseWithYandexuid() {
        String line = "tskv\t" +
                "ID_TYPE=YANDEX_UID\t" +
                "ID_VALUE=1269122101510161286\t" +
                "EMAIL=spbtester@yandex.ru\t" +
                "STATUS=UNCONFIRMED\t" +
                "SOURCE=CONFIRMATION\t" +
                "ACTIVE=false\t" +
                "MODIFICATION_TIME=54321\n";

        Collection<EmailOwnership> facts = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, facts.size());

        EmailOwnership expected = EmailOwnership.newBuilder()
                .setUid(
                        Uids.create(UidType.YANDEXUID, "1269122101510161286")
                )
                .setEmail("spbtester@yandex.ru")
                .setStatus("UNCONFIRMED")
                .setSource("CONFIRMATION")
                .setActive(false)
                .setModificationTime(54321)
                .build();

        assertEquals(expected, facts.iterator().next());
    }

    @Test
    public void testSkipIdWithUnkownIdType() {
        String line = "tskv\t" +
                "ID_TYPE=UNKNOWN\t" +
                "ID_VALUE=1269122101510161286\t" +
                "EMAIL=spbtester@yandex.ru\t" +
                "STATUS=UNCONFIRMED\t" +
                "SOURCE=CONFIRMATION\t" +
                "ACTIVE=false\t" +
                "MODIFICATION_TIME=12345\n";

        Collection<EmailOwnership> facts = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        assertEquals(0, facts.size());
    }
}
