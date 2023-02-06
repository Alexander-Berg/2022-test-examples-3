package ru.yandex.market.checkout.carter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.checkout.carter.config.TestCarterConfig;
import ru.yandex.market.checkout.carter.context.YdbReadWriteContainerContextInitializer;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.ItemPromo;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.dao.ydb.CarterYdbDao;
import ru.yandex.market.checkout.carter.utils.builders.ItemOfferBuilder;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.Color.WHITE;
import static ru.yandex.market.checkout.carter.model.UserIdType.YANDEXUID;

@ActiveProfiles({"test", "client-mock"})
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestCarterConfig.class, initializers = YdbReadWriteContainerContextInitializer.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@WebAppConfiguration
public abstract class CarterMockedDbTestBase {

    protected final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    @Autowired
    protected CarterYdbDao ydbDao;
    @Autowired
    private MemCachedAgentMockFactory mockFactory;
    @Autowired
    private MemCachedAgent memCachedAgentMock;
    @Autowired
    @Qualifier("clock")
    private TestableClock testableClock;

    @BeforeEach
    public void cleanDatabase() {
        ydbDao.truncateTable();
    }

    protected UserContext prepareTestData(String scriptName) {
        try {

            UserContext userContext = createUserContext("1");
            ObjectMapper objectMapper = new ObjectMapper();

            ItemOffer[] items = objectMapper.readValue(new ClassPathResource(scriptName).getFile(), ItemOffer[].class);
            ydbDao.bulkCreateItems(Set.of(items), userContext);

            return userContext;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    protected UserContext createUserContext(String userId) {
        return UserContext.of(
                OwnerKey.of(
                        Color.BLUE,
                        UserIdType.UID,
                        userId
                )
        );
    }

    @BeforeEach
    public void resetCache() {
        Mockito.reset(memCachedAgentMock);
        mockFactory.resetMemCachedAgentMock(memCachedAgentMock);
    }

    @AfterEach
    public void tearDown() {
        testableClock.clearFixed();
    }

    protected void createCartList(String userId, int itemCount, int itemPromoCount, int forAdult) {
        createCartList(userId, itemCount, itemPromoCount, forAdult, false, false);
    }

    protected void createCartList(String userId,
                                  int itemCount,
                                  int itemPromoCount,
                                  int forAdult,
                                  boolean withActualized) {
        createCartList(userId, itemCount, itemPromoCount, forAdult, withActualized, false);
    }

    protected void createCartList(String userId,
                                  int itemCount,
                                  int itemPromoCount,
                                  int forAdult,
                                  boolean withActualized,
                                  boolean withNullPromo) {
        UserContext userContext = UserContext.of(OwnerKey.of(WHITE, YANDEXUID, userId));
        CartList cartList = ydbDao.createCartList(userContext);

        Set<CartItem> items = new HashSet<>();
        for (int i = 0; i < itemCount; i++) {
            boolean adult = false;
            if (forAdult > i) {
                adult = true;
            }
            items.add(prepareCartItem(cartList.getId(), itemPromoCount, adult, withActualized, withNullPromo));
        }

        fillCartList(userContext, items);
    }

    protected void assertCartsEquals(CartList expected, CartList actual, boolean withTime) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getNote(), actual.getNote());
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getRgb(), actual.getRgb());
        assertEquals(expected.getOwnerId(), actual.getOwnerId());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getItems().size(), actual.getItems().size());

        if (withTime) {
            assertEquals(expected.getCreateTime(), actual.getCreateTime());
            assertEquals(expected.getUpdateTime(), actual.getUpdateTime());
        }

        assertItemsEquals(expected, actual);
    }

    protected void assertItemsEquals(CartList expected, CartList actual) {
        List<CartItem> expectedItems = expected.getItems();
        List<CartItem> actualItems = actual.getItems();
        expectedItems.sort(Comparator.comparingLong(CartItem::getId));
        actualItems.sort(Comparator.comparingLong(CartItem::getId));
        for (int i = 0; i < expectedItems.size(); i++) {
            CartItem expectedItem = expectedItems.get(i);
            CartItem actualItem = actualItems.get(i);
            assertEquals(expectedItem.getBundleId(), actualItem.getBundleId());
            assertEquals(expectedItem.getCount(), actualItem.getCount());
            assertEquals(expectedItem.getBundlePromoId(), actualItem.getBundlePromoId());
            assertEquals(expectedItem.getName(), actualItem.getName());
            assertEquals(expectedItem.getId(), actualItem.getId());
            assertEquals(expectedItem.getBenefit(), actualItem.getBenefit());
            assertEquals(expectedItem.getColor(), actualItem.getColor());
            assertEquals(expectedItem.getKind2Params(), actualItem.getKind2Params());
            assertEquals(expectedItem.getLabel(), actualItem.getLabel());
            assertEquals(expectedItem.getObjId(), actualItem.getObjId());
            assertEquals(expectedItem.getObjType(), actualItem.getObjType());
            if (expectedItem.getPromos() != null) {
                assertTrue(expectedItem.getPromos().containsAll(actualItem.getPromos()));
                assertTrue(actualItem.getPromos().containsAll(expectedItem.getPromos()));
            } else {
                assertNull(actualItem.getPromos());
            }
        }
    }

    protected ItemOffer prepareCartItem(long cartListId,
                                        int itemPromoCount,
                                        boolean adult,
                                        boolean withActualizedInfo) {

        var offer = prepareCartItem(cartListId, itemPromoCount, adult);

        if (withActualizedInfo) {
            offer.setActualizedPrice(new BigDecimal(RandomUtils.nextInt(100, 1000)));
            offer.setActualizedObjId(RandomStringUtils.randomAlphabetic(10));
        }

        return offer;
    }

    protected ItemOffer prepareCartItem(long cartListId,
                                        int itemPromoCount,
                                        boolean adult,
                                        boolean withActualizedInfo,
                                        boolean withNullPromo) {

        var offer = prepareCartItem(cartListId, itemPromoCount, adult, withActualizedInfo);

        if (withNullPromo) {
            offer.setPromos(null);
        }

        return offer;
    }

    protected ItemOffer prepareCartItem(long cartListId, int itemPromoCount, boolean adult) {
        Set<ItemPromo> promos = new HashSet<>(itemPromoCount);
        for (int j = 0; j < itemPromoCount; j++) {
            promos.add(new ItemPromo(
                    RandomStringUtils.randomAlphabetic(10),
                    RandomStringUtils.randomAlphabetic(10)
            ));
        }

        return new ItemOfferBuilder()
                .withObjType(CartItem.Type.OFFER)
                .withObjId(RandomStringUtils.randomAlphabetic(10))
                .withName(RandomStringUtils.randomAlphabetic(10))
                .withBundleId(RandomStringUtils.randomAlphabetic(10))
                .withListId(cartListId)
                .withHid(RandomUtils.nextLong())
                .withModelId(RandomUtils.nextLong())
                .withShopId(RandomUtils.nextLong())
                .withCount(RandomUtils.nextInt(1, 10))
                .withAdult(adult)
                .withPromos(promos)
                .build();
    }

    private void fillCartList(UserContext userContext, Set<CartItem> items) {
        if (!items.isEmpty()) {
            ydbDao.bulkCreateItems(items, userContext);
        }
    }
}
