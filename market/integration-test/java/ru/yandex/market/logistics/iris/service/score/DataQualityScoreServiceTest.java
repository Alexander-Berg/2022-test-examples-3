package ru.yandex.market.logistics.iris.service.score;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class DataQualityScoreServiceTest extends AbstractContextualTest {

    private static final ZonedDateTime UPDATED_DATE_TIME = ZonedDateTime.of(
            LocalDate.of(1970, 1, 2).atStartOfDay(),
            ZoneOffset.UTC
    );

    @Autowired
    private DataQualityScoreService dqScoreService;

    @SpyBean
    private UtcTimestampProvider utcTimestampProvider;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/1.xml")
    public void shouldSuccessReturnScore() {
        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.size()).isEqualTo(2);

            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isEqualTo(BigDecimal.valueOf(110));

            assertions.assertThat(score.get(ItemIdentifier.of("2", "sku2"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("2", "sku2"))).isEqualTo(BigDecimal.valueOf(127));

        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/2.xml")
    public void shouldReturnDefaultScore() {
        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.size()).isEqualTo(1);

            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isEqualTo(BigDecimal.valueOf(0));
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/3.xml")
    public void shouldReturnDefaultScoreIfScoreNull() {
        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.size()).isEqualTo(1);

            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isEqualTo(BigDecimal.valueOf(0));
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/4.xml")
    public void shouldReturnDefaultScoreIfReferenceIndexIsEmpty() {
        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.size()).isEqualTo(1);

            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isEqualTo(BigDecimal.valueOf(0));
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/5.xml")
    public void shouldNotReturnScoreIfReferenceIndexDoesNotExists() {
        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.isEmpty()).isTrue();
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/6.xml")
    public void shouldReturnDefaultScoreIfItemOneAlreadyScored() {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.size()).isEqualTo(2);

            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isEqualTo(BigDecimal.valueOf(0));

            assertions.assertThat(score.get(ItemIdentifier.of("2", "sku2"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("2", "sku2"))).isEqualTo(BigDecimal.valueOf(127));

        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/score/7.xml")
    public void shouldReturnDefaultScoreIfItemsAlreadyScored() {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        Map<ItemIdentifier, BigDecimal> score = dqScoreService.getScore(createItemIdentifier());

        assertSoftly(assertions -> {
            assertions.assertThat(score.size()).isEqualTo(2);

            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("1", "sku1"))).isEqualTo(BigDecimal.valueOf(0));

            assertions.assertThat(score.get(ItemIdentifier.of("2", "sku2"))).isNotNull();
            assertions.assertThat(score.get(ItemIdentifier.of("2", "sku2"))).isEqualTo(BigDecimal.valueOf(0));

        });
    }

    public Set<ItemIdentifier> createItemIdentifier() {
        return ImmutableSet.of(
                ItemIdentifier.of("1", "sku1"),
                ItemIdentifier.of("2", "sku2"));
    }
}
