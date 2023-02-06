package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.controller.dto.LoadSnapshotResponse;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.PromoYtGenerator;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.core.dao.bundle.PromoBundleDao;
import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.flash.FlashPromoService;
import ru.yandex.market.loyalty.core.utils.FlashPromoUtils;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.test.TestFor;
import Market.Promo.Promo.PromoDetails;

import javax.annotation.Nonnull;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.admin.utils.FlashPromoUtils.flashPromo;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.GenericBundleDSL.relation;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.blueFlashDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.giftWithPurchaseDetails;
import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.promoDetails;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;

@TestFor(UnsafePromoImportController.class)
public class UnsafePromoImportControllerTest extends MarketLoyaltyAdminMockedDbTest {

    private static final long FEED_ID = 123L;
    private static final String PROMO = "some promo";
    private static final String SNAPSHOT_KEY = "snapshot promo";
    private static final String FIRST_SSKU = "first ssku";
    private static final String SECOND_SSKU = "second ssku";

    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private PromoBundleService promoBundleService;
    @Autowired
    private FlashPromoDao flashPromoDao;
    @Autowired
    private PromoBundleDao promoBundleDao;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoYtTestHelper ytTestHelper;
    @Autowired
    private ObjectMapper objectMapper;

    private PromoDetails.Builder flashPromoDetails;
    private PromoDetails.Builder giftWithPurchasePromoDetails;

    @Before
    public void configure() {
        ZonedDateTime current = clock.dateTime().atZone(clock.getZone());

        giftWithPurchasePromoDetails = promoDetails(
                PromoYtGenerator.PromoDetailsDSL.shopPromoId(PROMO),
                PromoYtGenerator.PromoDetailsDSL.starts(current),
                PromoYtGenerator.PromoDetailsDSL.ends(current.plusDays(1)),

                giftWithPurchaseDetails(
                        relation(
                                PromoYtGenerator.GenericBundleDSL.primary(
                                        PromoYtGenerator.GenericBundleDSL.ssku(FIRST_SSKU)
                                ),
                                PromoYtGenerator.GenericBundleDSL.gift(
                                        PromoYtGenerator.GenericBundleDSL.item(
                                                PromoYtGenerator.GenericBundleDSL.ssku(SECOND_SSKU)
                                        )
                                ),
                                PromoYtGenerator.GenericBundleDSL.proportion(100)
                        )
                )
        );

        flashPromoDetails = promoDetails(
                PromoYtGenerator.PromoDetailsDSL.shopPromoId(PROMO),
                PromoYtGenerator.PromoDetailsDSL.starts(current),
                PromoYtGenerator.PromoDetailsDSL.ends(current.plusDays(1)),

                blueFlashDetails(
                        PromoYtGenerator.BlueFlashDSL.item(
                                PromoYtGenerator.BlueFlashDSL.offer(FEED_ID, FIRST_SSKU),
                                PromoYtGenerator.BlueFlashDSL.fixedPrice(120),
                                PromoYtGenerator.BlueFlashDSL.quantityBudget(5),
                                PromoYtGenerator.BlueFlashDSL.moneyBudget(200)
                        ),
                        PromoYtGenerator.BlueFlashDSL.item(
                                PromoYtGenerator.BlueFlashDSL.offer(FEED_ID, SECOND_SSKU),
                                PromoYtGenerator.BlueFlashDSL.fixedPrice(120),
                                PromoYtGenerator.BlueFlashDSL.quantityBudget(5),
                                PromoYtGenerator.BlueFlashDSL.moneyBudget(200)
                        )
                )
        );
    }

    @Test
    public void shouldNotLoadPromoSnapshotIfNotExists() {
        final LoadSnapshotResponse loadSnapshotResponse = ytTestHelper.withMock(
                db -> db.promo(FEED_ID, SNAPSHOT_KEY, flashPromoDetails.clone()), this::loadSnapshot
        );

        assertThat(loadSnapshotResponse, notNullValue());
        assertThat(loadSnapshotResponse.getResults(), hasSize(1));
        assertThat(loadSnapshotResponse.getResults(), hasItem(allOf(
                hasProperty("promoKey", is(SNAPSHOT_KEY)),
                hasProperty("success", is(false))
        )));

        Set<FlashPromoDescription> flashPromoDescriptions = flashPromoDao.select(
                FlashPromoDescription.FEED_ID.eqTo(FEED_ID),
                FlashPromoDescription.SHOP_PROMO_ID.eqTo(PROMO),
                FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true),
                FlashPromoDao.activeOn(clock.dateTime())
        );

        assertThat(flashPromoDescriptions, empty());
    }

    @Test
    public void shouldLoadFlashPromoSnapshotVersion() {
        final FlashPromoDescription actualPromo = flashPromoService.createPromo(flashPromo(
                FlashPromoUtils.feedId(FEED_ID),
                FlashPromoUtils.promoKey(PROMO),
                FlashPromoUtils.shopPromoId(PROMO)
        ));

        final LoadSnapshotResponse loadSnapshotResponse = ytTestHelper.withMock(
                db -> db.promo(FEED_ID, SNAPSHOT_KEY, flashPromoDetails.clone()), this::loadSnapshot
        );

        assertThat(loadSnapshotResponse, notNullValue());
        assertThat(loadSnapshotResponse.getResults(), hasSize(1));
        assertThat(loadSnapshotResponse.getResults(), hasItem(allOf(
                hasProperty("promoKey", is(SNAPSHOT_KEY)),
                hasProperty("success", is(true))
        )));

        Set<FlashPromoDescription> flashPromoDescriptions = flashPromoDao.select(
                FlashPromoDescription.FEED_ID.eqTo(FEED_ID),
                FlashPromoDescription.SHOP_PROMO_ID.eqTo(PROMO),
                FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true),
                FlashPromoDao.activeOn(clock.dateTime())
        );

        assertThat(flashPromoDescriptions, hasSize(1));
        assertThat(flashPromoDescriptions, hasItem(
                hasProperty("id", not(comparesEqualTo(actualPromo.getId())))
        ));
    }

    @Test
    public void shouldLoadGiftWithPurchasePromoSnapshotVersion() {
        final PromoBundleDescription actualPromo = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoId(PROMO),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, FIRST_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        PromoBundleUtils.when(FIRST_SSKU),
                        then(SECOND_SSKU),
                        proportion(40)
                ))
        ));

        final LoadSnapshotResponse loadSnapshotResponse = ytTestHelper.withMock(
                db -> db.promo(FEED_ID, SNAPSHOT_KEY, giftWithPurchasePromoDetails.clone()), this::loadSnapshot
        );

        assertThat(loadSnapshotResponse, notNullValue());
        assertThat(loadSnapshotResponse.getResults(), hasSize(1));
        assertThat(loadSnapshotResponse.getResults(), hasItem(allOf(
                hasProperty("promoKey", is(SNAPSHOT_KEY)),
                hasProperty("success", is(true))
        )));

        Set<PromoBundleDescription> flashPromoDescriptions = promoBundleDao.select(
                PromoBundleDescription.FEED_ID.eqTo(FEED_ID),
                PromoBundleDescription.SHOP_PROMO_ID.eqTo(PROMO),
                PromoBundleDescription.SNAPSHOT_VERSION.eqTo(true),
                PromoBundleDao.activeOn(clock.dateTime())
        );

        assertThat(flashPromoDescriptions, hasSize(1));
        assertThat(flashPromoDescriptions, hasItem(
                hasProperty("id", not(comparesEqualTo(actualPromo.getId())))
        ));
    }

    @Nonnull
    private LoadSnapshotResponse loadSnapshot() {
        try {
            String contentAsString = mockMvc
                    .perform(post("/api/unsafe/promos/yt/loadSnapshot")
                            .param("promoKey", SNAPSHOT_KEY)
                            .param("snapshotPath", "hahn://test/snapshotPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(csrf()))
                    .andDo(log())
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            return objectMapper.readValue(contentAsString, LoadSnapshotResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
