package ru.yandex.market.core.notification.service.resolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.core.agency.ContactAndAgencyUserService;
import ru.yandex.market.core.campaign.IdsResolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.matcher.CollectionEqualsMatcher.equalToCollection;

/**
 * Тесты для {@link CampaignsResolver}.
 *
 * @author avetokhin 30/08/16.
 */
public class CampaignsResolverTest {

    private static final long SHOP_ID = 666L;
    private static final long BUSINESS_ID = 888L;
    private static final long USER_ID = 13L;

    private static final Long CAMPAIGN_BY_SHOP = 777L;
    private static final Long CAMPAIGN_BY_BUSINESS = 999L;
    private static final List<Long> CAMPAIGNS_BY_UID = Arrays.asList(8L, 9L);
    private static final List<Long> ALL_CAMPAIGNS = Arrays.asList(777L, 8L, 9L);

    /**
     * Без указания shop_id и user_id.
     */
    @Test
    public void resolveWithNullParams() {
        test((resolver, idsResolver) -> {
            final Collection<Long> campaigns = resolver.resolve(null, null);
            assertThat(campaigns, notNullValue());
            assertThat(campaigns, hasSize(0));
        });
    }

    /**
     * С указанием только shop_id.
     */
    @Test
    public void resolveWithShopId() {
        test((resolver, idsResolver) -> {
            final Collection<Long> campaigns = resolver.resolve(SHOP_ID, null);
            assertThat(campaigns, notNullValue());
            assertThat(campaigns, equalToCollection(Collections.singleton(CAMPAIGN_BY_SHOP)));
        });
    }

    /**
     * С указанием только user_id.
     */
    @Test
    public void resolveWithUserId() {
        test((resolver, idsResolver) -> {
            final Collection<Long> campaigns = resolver.resolve(null, USER_ID);
            assertThat(campaigns, notNullValue());
            assertThat(campaigns, equalToCollection(CAMPAIGNS_BY_UID));
        });
    }

    /**
     * С указанием только business_id.
     */
    @Test
    public void resolveWithBusinessId() {
        test((resolver, idsResolver) -> {
            final Collection<Long> campaigns = resolver.resolve(BUSINESS_ID, null);
            assertThat(campaigns, notNullValue());
            assertThat(campaigns, equalToCollection(Collections.singleton(CAMPAIGN_BY_BUSINESS)));
        });
    }

    /**
     * С указанием и shop_id и user_id.
     */
    @Test
    public void resolveWithShopIdAndUserId() {
        test((resolver, idsResolver) -> {
            final Collection<Long> campaigns = resolver.resolve(SHOP_ID, USER_ID);
            assertThat(campaigns, notNullValue());
            assertThat(campaigns, equalToCollection(ALL_CAMPAIGNS));
        });
    }

    private void test(final CampaignResolverTester tester) {
        final IdsResolver idsResolver = mock(IdsResolver.class);
        when(idsResolver.getCampaignId(SHOP_ID)).thenReturn(CAMPAIGN_BY_SHOP);

        when(idsResolver.getCampaignId(BUSINESS_ID)).thenReturn(CAMPAIGN_BY_BUSINESS);

        final ContactAndAgencyUserService contactAndAgencyUserService = mock(ContactAndAgencyUserService.class);
        when(contactAndAgencyUserService.getLinkedCampaignIdsByUid(anyLong())).thenReturn(CAMPAIGNS_BY_UID);

        final CampaignsResolver resolver = new CampaignsResolver(contactAndAgencyUserService, idsResolver);
        tester.test(resolver, idsResolver);
    }

    /**
     * Интерфейс для точечного тестирования.
     */
    private interface CampaignResolverTester {
        void test(final CampaignsResolver resolver, final IdsResolver idsResolver);
    }
}
