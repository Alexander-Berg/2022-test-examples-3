package ru.yandex.market.checkout.checkouter.actualization.fetchers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.json.BnplDenialReason;
import ru.yandex.market.common.report.model.json.credit.BnplDenial;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;
import ru.yandex.market.common.report.model.json.credit.CreditOffer;
import ru.yandex.market.common.report.model.json.credit.InstallmentsInfo;
import ru.yandex.market.common.report.model.json.credit.MonthlyPayment;
import ru.yandex.market.common.report.model.json.credit.YandexBnplInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.TOO_CHEAP;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.TOO_EXPENSIVE;

class ReportCreditInfoFetcherTest {

    private final ReportCreditInfoFetcher reportCreditInfoFetcher = new ReportCreditInfoFetcher();

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void accumulateCreditInfoFromOffersTest(
            String name,
            List<FoundOffer> offers,
            Map<FeedOfferId, Integer> feedOfferIdIntegerMap,
            CreditInfo expectedResult
    ) {
        assertThat(reportCreditInfoFetcher.accumulateCreditInfoFromOffers(offers, feedOfferIdIntegerMap))
                .isEqualTo(expectedResult);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                Arguments.of(
                        "Рассрочка только на 6 меcяцев",
                        List.of(
                                createFoundOffer(
                                        "1",
                                        "1.0",
                                        1,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., true),
                                                createInstallmentsInfo(BigDecimal.valueOf(11), 12., null)
                                        ),
                                        createYandexBnplInfo(true, null, null)
                                ),
                                createFoundOffer(
                                        "2",
                                        "2.0",
                                        2,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., null),
                                                createInstallmentsInfo(BigDecimal.valueOf(12), 13., false)
                                        ),
                                        createYandexBnplInfo(false, TOO_EXPENSIVE, new BigDecimal(1000))
                                )
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 1,
                                FeedOfferId.from(2, "2"), 1
                        ),
                        createCreditInfo(
                                List.of(
                                        createCreditOffer("1.0", 1, createYandexBnplInfo(true, null, null)),
                                        createCreditOffer("2.0", 2, createYandexBnplInfo(false, TOO_EXPENSIVE,
                                                new BigDecimal(1000)))
                                ),
                                Set.of(
                                        createInstallmentsInfo(BigDecimal.valueOf(20), 6., null)
                                )
                        )
                ),
                Arguments.of(
                        "null test",
                        List.of(
                                createFoundOffer("1",
                                        "1.0",
                                        1,
                                        null, null)
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 1
                        ),
                        createCreditInfo(
                                List.of(createCreditOffer("1.0", 1, new YandexBnplInfo())),
                                Collections.emptySet()
                        )
                ),
                Arguments.of(
                        "Found offer без рассрочки + Found offer с рассрочкой = нет рассрочки",
                        List.of(
                                createFoundOffer(
                                        "1",
                                        "1.0",
                                        1,
                                        null,
                                        createYandexBnplInfo(false, TOO_CHEAP,
                                                new BigDecimal(1000))),
                                createFoundOffer(
                                        "2",
                                        "2.0",
                                        2,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., true),
                                                createInstallmentsInfo(BigDecimal.valueOf(12), 13., false)
                                        ),
                                        createYandexBnplInfo(true, null, null)
                                )
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 1,
                                FeedOfferId.from(2, "2"), 1
                        ),
                        createCreditInfo(
                                List.of(
                                        createCreditOffer("1.0", 1,
                                                createYandexBnplInfo(false, TOO_CHEAP, new BigDecimal(1000))),
                                        createCreditOffer("2.0", 2, createYandexBnplInfo(true, null, null))
                                ),
                                Collections.emptySet()
                        )
                ),
                Arguments.of(
                        "Рассрочка только на 6, 12 меcяцев, разные значения bnplAvaliable",
                        List.of(
                                createFoundOffer(
                                        "1",
                                        "1.0",
                                        1,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., true),
                                                createInstallmentsInfo(BigDecimal.valueOf(11), 12., true)
                                        ),
                                        createYandexBnplInfo(true, null, null)),
                                createFoundOffer(
                                        "2",
                                        "2.0",
                                        2,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., null),
                                                createInstallmentsInfo(BigDecimal.valueOf(11), 12., null)
                                        ),
                                        createYandexBnplInfo(false, TOO_CHEAP, new BigDecimal(1000)))
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 1,
                                FeedOfferId.from(2, "2"), 1
                        ),
                        createCreditInfo(
                                List.of(
                                        createCreditOffer("1.0", 1, createYandexBnplInfo(true, null, null)),
                                        createCreditOffer("2.0", 2, createYandexBnplInfo(false, TOO_CHEAP,
                                                new BigDecimal(1000)))
                                ),
                                Set.of(
                                        createInstallmentsInfo(BigDecimal.valueOf(20), 6., null),
                                        createInstallmentsInfo(BigDecimal.valueOf(22), 12., null)
                                )
                        )
                ),
                Arguments.of(
                        "разные значения bnplAvaliable",
                        List.of(
                                createFoundOffer(
                                        "1",
                                        "1.0",
                                        1,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., false),
                                                createInstallmentsInfo(BigDecimal.valueOf(11), 12., null),
                                                createInstallmentsInfo(null, null, true)
                                        ),
                                        createYandexBnplInfo(true, null, null)
                                ),
                                createFoundOffer(
                                        "2",
                                        "2.0",
                                        2,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., true),
                                                createInstallmentsInfo(BigDecimal.valueOf(11), 12., null),
                                                createInstallmentsInfo(null, null, false)
                                        ),
                                        createYandexBnplInfo(true, null, null)
                                )
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 1,
                                FeedOfferId.from(2, "2"), 1
                        ),
                        createCreditInfo(
                                List.of(
                                        createCreditOffer("1.0", 1, createYandexBnplInfo(true, null, null)),
                                        createCreditOffer("2.0", 2, createYandexBnplInfo(true, null, null))
                                ),
                                Set.of(
                                        createInstallmentsInfo(BigDecimal.valueOf(20), 6., null),
                                        createInstallmentsInfo(BigDecimal.valueOf(22), 12., null)
                                )
                        )
                ),
                Arguments.of(
                        "Рассрочка на 6,12 меcяцев c каунтом отличным от 1",
                        List.of(
                                createFoundOffer(
                                        "1",
                                        "1.0",
                                        1,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., true),
                                                createInstallmentsInfo(BigDecimal.valueOf(11), 12., null)
                                        ),
                                        createYandexBnplInfo(true, null, null)),
                                createFoundOffer(
                                        "2",
                                        "2.0",
                                        2,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., null),
                                                createInstallmentsInfo(BigDecimal.valueOf(12), 12., false)
                                        ),
                                        createYandexBnplInfo(false, TOO_CHEAP, new BigDecimal(1000))
                                )
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 4,
                                FeedOfferId.from(2, "2"), 6
                        ),
                        createCreditInfo(
                                List.of(
                                        createCreditOffer("1.0", 1, createYandexBnplInfo(true, null, null)),
                                        createCreditOffer("2.0", 2, createYandexBnplInfo(false, TOO_CHEAP,
                                                new BigDecimal(1000)))
                                ),
                                Set.of(
                                        createInstallmentsInfo(BigDecimal.valueOf(40 + 60), 6., null),
                                        createInstallmentsInfo(BigDecimal.valueOf(44 + 72), 12., null)
                                )
                        )
                ),
                Arguments.of(
                        "Null в yandexBnplInfo",
                        List.of(
                                createFoundOffer(
                                        "1",
                                        "1.0",
                                        1,
                                        Set.of(
                                                createInstallmentsInfo(BigDecimal.valueOf(10), 6., true)
                                        ), null)
                        ),
                        Map.of(
                                FeedOfferId.from(1, "1"), 1
                        ),
                        createCreditInfo(
                                List.of(
                                        createCreditOffer("1.0", 1, new YandexBnplInfo())
                                ),
                                Set.of(
                                        createInstallmentsInfo(BigDecimal.valueOf(10), 6., false)
                                )
                        )
                )
        );
    }

    private static CreditOffer createCreditOffer(
            String wareMd5,
            Integer hid,
            YandexBnplInfo yandexBnplInfo
    ) {
        var creditOffer = new CreditOffer();
        creditOffer.setYandexBnplInfo(yandexBnplInfo);
        creditOffer.setHid(hid);
        creditOffer.setWareId(wareMd5);
        return creditOffer;
    }

    private static InstallmentsInfo createInstallmentsInfo(
            BigDecimal monthlyPayment,
            Double term,
            Boolean bnplAvailable
    ) {
        var installmentsInfo = new InstallmentsInfo();
        installmentsInfo.setTerm(term);
        installmentsInfo.setBnplAvailable(bnplAvailable);
        if (monthlyPayment != null) {
            var payment = new MonthlyPayment();
            payment.setCurrency("RUR");
            payment.setValue(monthlyPayment);
            installmentsInfo.setMonthlyPayment(payment);
        }
        return installmentsInfo;
    }

    private static CreditInfo createCreditInfo(
            List<CreditOffer> creditOffers,
            Set<InstallmentsInfo> installmentsInfos
    ) {
        var creditInfo = new CreditInfo();
        creditInfo.setCreditOffers(creditOffers);
        creditInfo.setInstallmentsInfoSet(installmentsInfos);
        return creditInfo;
    }

    private static FoundOffer createFoundOffer(
            String name,
            String wareMd5,
            Integer hid,
            Set<InstallmentsInfo> installments,
            YandexBnplInfo yandexBnplInfo) {
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setHyperCategoryId(hid);
        foundOffer.setWareMd5(wareMd5);
        foundOffer.setName(name);
        foundOffer.setInstallmentsInfoSet(installments);
        foundOffer.setYandexBnplInfo(yandexBnplInfo);

        foundOffer.setFeedId(Long.valueOf(hid));
        foundOffer.setShopOfferId(name);
        return foundOffer;
    }

    private static YandexBnplInfo createYandexBnplInfo(boolean enabled, BnplDenialReason reason, BigDecimal threshold) {
        YandexBnplInfo yandexBnplInfo = new YandexBnplInfo();
        yandexBnplInfo.setEnabled(reason == null);
        if (!yandexBnplInfo.isEnabled()) {
            BnplDenial bnplDenial = new BnplDenial();
            bnplDenial.setReason(reason);
            bnplDenial.setThreshold(threshold);
            yandexBnplInfo.setBnplDenial(bnplDenial);
        }
        return yandexBnplInfo;
    }
}
