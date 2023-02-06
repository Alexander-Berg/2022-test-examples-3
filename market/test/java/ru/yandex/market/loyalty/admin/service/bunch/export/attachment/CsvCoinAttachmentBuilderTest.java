package ru.yandex.market.loyalty.admin.service.bunch.export.attachment;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.attachment.AttachmentBuilder;
import ru.yandex.market.loyalty.core.model.attachment.AttachmentEntity;
import ru.yandex.market.loyalty.core.model.attachment.Field;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.NO_AUTH;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.dao.coin.CoinDao.DISCOUNT_TABLE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.NEW_LINE_CRNL;

public class CsvCoinAttachmentBuilderTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String MAIL = "unzip@example.com";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private BunchRequestService coinBunchRequestService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private CoinDao coinDao;

    @Test
    public void shouldUnzip() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(300_000))
        );

        coinBunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getId(),
                        "coin",
                        100,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        coinBunchRequestService.processScheduledRequests(500, Duration.ofNanos(TimeUnit.MINUTES.toNanos(15)),
                GeneratorType.COIN);

        List<AttachmentEntity> coins;
        byte[] fileAsBytes;
        try (AttachmentBuilder<AttachmentEntity> builder = new CsvAttachmentBuilder<>("test", Collections.singletonList(
                new Field<>("Ссылка активации", AttachmentEntity::getActivationToken)
        ))) {
            coins = coinService.search.buildCoins(coinDao.getCoins(DISCOUNT_TABLE.sourceKey.like("%coin%"), false))
                    .stream()
                    .map(coin -> AttachmentEntity.fromCoin(coin,
                            coinDao.getCoinDescriptionById(coin.getCoinDescriptionId())))
                    .collect(Collectors.toList());
            builder.init();
            builder.addEntities(coins);
            fileAsBytes = builder.build().getFileAsBytes();
        }

        List<String> tokens = readZipFile(fileAsBytes)
                .skip(1) // header
                .collect(Collectors.toList());
        assertThat(tokens, hasSize(100));
        assertThat(tokens, containsInAnyOrder(coins
                        .stream()
                        .map(AttachmentEntity::getActivationToken)
                        .map(Matchers::equalTo)
                        .collect(Collectors.toList())
                )
        );
    }

    public static Stream<String> readZipFile(byte[] fileAsBytes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileAsBytes));
             ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            zis.getNextEntry();
            IOUtils.copy(zis, fos);
            ZipEntry zipEntry = zis.getNextEntry();
            assertThat(zipEntry, is(nullValue()));

            return Arrays.stream(NEW_LINE_CRNL.split(new String(fos.toByteArray(), StandardCharsets.UTF_8)));
        }
    }
}
