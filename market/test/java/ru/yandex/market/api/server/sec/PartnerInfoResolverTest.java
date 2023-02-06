package ru.yandex.market.api.server.sec;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.PartnerInfo;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.PartnerInfoResolver;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class PartnerInfoResolverTest extends UnitTestBase {


    @Test
    public void shouldNotResolveForCommonTariff() {
        HttpServletRequest request = MockRequestBuilder.start().build();

        Client client = new Client() {{
            setTariff(TestTariffs.BASE);
        }};

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNull(partnerInfo);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldResolvePartnerIdAndVidForAllowClid() {
        Client client = new Client() {{
            setTariff(TestTariffs.CUSTOM);
            setAllowClid(true);
            setDistrType(789L);
        }};

        HttpServletRequest request = MockRequestBuilder.start()
                .param("clid", "123")
                .param("vid", "987")
                .build();

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNotNull(partnerInfo);
        assertEquals("123", partnerInfo.getClid());
        assertEquals("987", partnerInfo.getVid());
        assertEquals(789L, (long) partnerInfo.getDistrType());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldResolvePartnerIdAndVid2ForAllowClid() {
        Client client = new Client() {{
            setTariff(TestTariffs.CUSTOM);
            setAllowClid(true);
            setDistrType(789L);
        }};

        HttpServletRequest request = MockRequestBuilder.start()
                .param("clid", "123-321")
                .param("vid", "987")
                .build();

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNotNull(partnerInfo);
        assertEquals("123", partnerInfo.getClid());
        assertEquals("987", partnerInfo.getVid());
        assertEquals(789L, (long) partnerInfo.getDistrType());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldResolvePartnerIdForAllowClid() {
        Client client = new Client() {{
            setTariff(TestTariffs.CUSTOM);
            setAllowClid(true);
            setDistrType(789L);
        }};

        HttpServletRequest request = MockRequestBuilder.start()
            .param("clid", "123-456")
            .build();

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNotNull(partnerInfo);
        assertEquals("123", partnerInfo.getClid());
        assertEquals(789L, (long) partnerInfo.getDistrType());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldResolvePartnerIdFromClientForNotAllowedClid() {
        Client client = new Client() {{
            setTariff(TestTariffs.CUSTOM);
            setAllowClid(false);
            setClid("123");
        }};

        HttpServletRequest request = MockRequestBuilder.start()
            .param("clid", "0")
            .build();

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNotNull(partnerInfo);
        assertEquals("123", partnerInfo.getClid());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldResolvePartnerIdForAllowMClid() {
        Client client = new Client() {{
            setTariff(TestTariffs.CUSTOM);
            setAllowMclid(true);
        }};

        HttpServletRequest request = MockRequestBuilder.start()
            .param("mclid", "123")
            .build();

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNotNull(partnerInfo);
        assertEquals(123, (long) partnerInfo.getMclid());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldResolveMclidFromClientForNotAllowedMClid() {
        Client client = new Client() {{
            setTariff(TestTariffs.CUSTOM);
            setAllowMclid(false);
            setMclid(123L);
        }};

        HttpServletRequest request = MockRequestBuilder.start()
            .param("mclid", "0")
            .build();

        ValidationErrors errors = new ValidationErrors();

        PartnerInfo partnerInfo = PartnerInfoResolver.resolvePartnerInfo(request, client, errors);

        assertNotNull(partnerInfo);
        assertEquals(123, (long) partnerInfo.getMclid());
        assertTrue(errors.isEmpty());
    }
}
