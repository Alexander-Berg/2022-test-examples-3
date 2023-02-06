package ru.yandex.direct.jobs.adfox.messaging.handler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.adfoxmessaging.protos.AdfoxDealCreatePayload;
import ru.yandex.direct.adfoxmessaging.protos.AdfoxDealStatus;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.deal.model.DealType;
import ru.yandex.direct.core.entity.deal.model.StatusAdfox;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.jobs.adfox.messaging.handler.AdfoxDealConverter.PREFERRED_DEAL;

@Disabled("Не работающая фича 'частные сделки'")
class AdfoxDealConverterTest {

    private static final long DEAL_EXPORT_ID = 42L;
    private static final long AGENCY_ID = 43L;
    private static final Currency CURRENCY = CurrencyCode.RUB.getCurrency();
    private static final String DEAL_NAME = "Deal name";
    private static final String DEAL_DESCRIPTION = "Deal description";
    private static final String TARGETING_TEXT = "Targeting text";
    private static final long EXPECTED_IMPRESSIONS_PER_WEEK = 250L;
    private static final long EXPECTED_MONEY_PER_WEEK = 1000L;
    private static final String PUBLISHER_NAME = "Publisher name";
    private static final String CONTACTS = "Contacts";
    private static final long ONE_HUNDRED_IN_MILLIS = 100_000_000L;
    private static final long TEN_PERCENT_IN_MILLIS = 100_000L;
    private static final Percent TEN_PERCENT = Percent.fromPercent(BigDecimal.TEN);
    // Задаём '100' в таком виде, чтобы было равенство BigDecimal
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100_000_000L, 6);
    private static final BigDecimal ONE_HUNDRED_IN_CPM = BigDecimal.valueOf(100_000_000L, 3);

    private AdfoxDealConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AdfoxDealConverter();
    }

    @Test
    void convertAdfoxDealCreate_success_empty() {
        AdfoxDealCreatePayload adfoxDeal = AdfoxDealCreatePayload.newBuilder()
                .setStatus(AdfoxDealStatus.created)
                .setType(PREFERRED_DEAL)
                .setCurrencyId(CURRENCY.getIsoNumCode().toString())
                .setCpm(ONE_HUNDRED_IN_MILLIS)
                .setMarginRatio(TEN_PERCENT_IN_MILLIS)
                .build();

        Deal actual = converter.convertAdfoxDealCreate(adfoxDeal);

        Deal expected = new Deal();
        expected.withAdfoxStatus(StatusAdfox.CREATED)
                .withDealType(DealType.PREFERRED_DEAL)
                .withCurrencyCode(CURRENCY.getCode())
                .withCpm(ONE_HUNDRED_IN_CPM)
                .withMarginRatio(TEN_PERCENT)
                .withDealJson("{}")
                .withPlacements(Collections.emptyList())
                .withAdfoxDescription("");

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "dateCreated");
    }

    @Test
    void convertAdfoxDealCreate_success() {
        AdfoxDealCreatePayload adfoxDeal = AdfoxDealCreatePayload.newBuilder()
                .setDealExportId(DEAL_EXPORT_ID)
                .setAgencyId(AGENCY_ID)
                .setStatus(AdfoxDealStatus.created)
                .setType(PREFERRED_DEAL)
                .setCurrencyId(CURRENCY.getIsoNumCode().toString())
                .setName(DEAL_NAME)
                .setDescription(DEAL_DESCRIPTION)
                .setTargetingsText(TARGETING_TEXT)
                .setExpectedImpressionsPerWeek(EXPECTED_IMPRESSIONS_PER_WEEK)
                .setExpectedMoneyPerWeek(EXPECTED_MONEY_PER_WEEK)
                .setPublisherName(PUBLISHER_NAME)
                .setContacts(CONTACTS)
                .setDateTimeStartUtc("2017-11-20T14:45:00Z")
                .setDateTimeEndUtc("2018-11-20T14:45:00Z")
                .setCpm(ONE_HUNDRED_IN_MILLIS)
                .setAgencyRevenueRatio(TEN_PERCENT_IN_MILLIS)
                .setMarginRatio(TEN_PERCENT_IN_MILLIS)
                .setAdfoxSpecials(Struct.newBuilder()
                        .putFields("key", Value.newBuilder().setStringValue("value").build()))
                .addPlacements(AdfoxDealCreatePayload.Placement.newBuilder()
                        .setPageId(101)
                        .addAllImpId(asList(201L, 202L))
                        .build())
                .build();

        Deal actual = converter.convertAdfoxDealCreate(adfoxDeal);

        Deal expected = new Deal();
        expected.withId(DEAL_EXPORT_ID)
                .withClientId(AGENCY_ID)
                .withAdfoxStatus(StatusAdfox.CREATED)
                .withDealType(DealType.PREFERRED_DEAL)
                .withCurrencyCode(CURRENCY.getCode())
                .withAdfoxName(DEAL_NAME)
                .withAdfoxDescription(DEAL_DESCRIPTION)
                .withTargetingsText(TARGETING_TEXT)
                .withExpectedImpressionsPerWeek(EXPECTED_IMPRESSIONS_PER_WEEK)
                .withExpectedMoneyPerWeek(EXPECTED_MONEY_PER_WEEK)
                .withPublisherName(PUBLISHER_NAME)
                .withContacts(CONTACTS)
                .withDateStart(LocalDateTime.of(2017, Month.NOVEMBER, 20, 14, 45))
                .withDateEnd(LocalDateTime.of(2018, Month.NOVEMBER, 20, 14, 45))
                .withCpm(ONE_HUNDRED_IN_CPM)
                .withAgencyFeePercent(TEN_PERCENT)
                .withMarginRatio(TEN_PERCENT)
                .withPlacements(Collections.singletonList(new DealPlacement()
                        .withPageId(101L).withImpId(Arrays.asList(201L, 202L))))
                .withDealJson("{\"adfoxSpecials\":{\"key\":\"value\"}}");

        // Для информации: поля name и description не заполняем. Туда записывается информация, заданная агенством
        expected.withName(null)
                .withDescription(null);

        assertThat(actual)
                .isEqualToIgnoringGivenFields(expected, "dateCreated");
    }

    @Test
    void zeroToNull_success() {
        Long actual = converter.zeroToNull(42L);
        assertThat(actual).isEqualTo(42L);
    }

    @Test
    void zeroToNull_null_whenZero() {
        Long actual = converter.zeroToNull(0L);
        assertThat(actual).isNull();
    }

    @ParameterizedTest
    @MethodSource("parametersForConvertStatus_success")
    void convertStatus_success(AdfoxDealStatus transportStatus, StatusAdfox coreStatus) {
        StatusAdfox actual = converter.convertStatus(transportStatus);
        assertThat(actual).isEqualTo(coreStatus);
    }

    static Collection<Object[]> parametersForConvertStatus_success() {
        return Arrays.asList(new Object[][]{
                {AdfoxDealStatus.created, StatusAdfox.CREATED},
                {AdfoxDealStatus.active, StatusAdfox.ACTIVE},
                {AdfoxDealStatus.closed, StatusAdfox.CLOSED},
        });
    }

    @ParameterizedTest
    @MethodSource("parametersForConvertStatus_error")
    void convertStatus_error(AdfoxDealStatus transportStatus) {
        assertThatThrownBy(() -> converter.convertStatus(transportStatus))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Collection<AdfoxDealStatus> parametersForConvertStatus_error() {
        EnumSet<AdfoxDealStatus> knownDealStatuses =
                EnumSet.of(AdfoxDealStatus.created, AdfoxDealStatus.active, AdfoxDealStatus.closed);
        return StreamEx.of(AdfoxDealStatus.values())
                .remove(knownDealStatuses::contains)
                .toList();
    }

    @ParameterizedTest
    @CsvSource({
            "10, PREFERRED_DEAL",
            "11, PRIVATE_MARKETPLACE"
    })
    void convertType_success(Integer transportDealType, String coreDealTypeString) {
        DealType actual = converter.convertType(transportDealType);
        assertThat(actual.name()).isEqualTo(coreDealTypeString);
    }

    @Test
    void convertType_error() {
        int unknownType = 0;
        assertThatThrownBy(() -> converter.convertType(unknownType))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertCurrencyId_success() {
        CurrencyCode actual = converter.convertCurrencyId(CURRENCY.getIsoNumCode().toString());
        assertThat(actual).isEqualTo(CURRENCY.getCode());
    }

    @Test
    void convertCurrencyId_error_whenCurrencyIdIsUnknown() {
        String unknownIsoCode = "12345";
        assertThatThrownBy(() -> converter.convertCurrencyId(unknownIsoCode))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(unknownIsoCode);
    }

    @Test
    void convertTimestamp_success() {
        LocalDateTime actual = converter.convertTimestamp("2017-11-20T10:30:00Z");
        LocalDateTime expected = LocalDateTime.of(2017, Month.NOVEMBER, 20, 10, 30);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void convertTimestamp_null_onBlank() {
        LocalDateTime actual = converter.convertTimestamp("");
        assertThat(actual).isNull();
    }


    @Test
    void convertNumberFromMicros_success() {
        BigDecimal actual = converter.convertNumberFromMicros(ONE_HUNDRED_IN_MILLIS);
        assertThat(actual).isEqualByComparingTo(ONE_HUNDRED);
    }

    @Test
    void convertRatioFromMicros_success() {
        Percent actual = converter.convertRatioFromMicros(TEN_PERCENT_IN_MILLIS);
        assertThat(actual).isEqualTo(TEN_PERCENT);
    }

    @Test
    void convertCpmFromMicros_success() {
        BigDecimal actual = converter.convertCpmFromMicros(ONE_HUNDRED_IN_MILLIS);
        assertThat(actual).isEqualByComparingTo(ONE_HUNDRED_IN_CPM);
    }

    /**
     * @see CreateDealMiscFieldsConverterTest
     */
    @Test
    void extractMisc_success_onEmpty() {
        // дублируется тест из CreateDealMiscFieldsConverterTest
        AdfoxDealCreatePayload emptyPayload = AdfoxDealCreatePayload.newBuilder().build();
        String actual = converter.extractMisc(emptyPayload);
        assertThat(actual).isEqualTo("{}");
    }
}
