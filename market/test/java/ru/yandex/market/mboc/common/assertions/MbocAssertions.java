package ru.yandex.market.mboc.common.assertions;

import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.SoftAssertions;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileAssertions;
import ru.yandex.market.mboc.common.assertions.custom.IssueAssertions;
import ru.yandex.market.mboc.common.assertions.custom.IterableGeneralOffersAssert;
import ru.yandex.market.mboc.common.assertions.custom.ListGeneralOfferAssertions;
import ru.yandex.market.mboc.common.assertions.custom.OfferAssertions;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author s-ermakov
 */
public class MbocAssertions {

    protected MbocAssertions() {
    }

    public static ExcelFileAssertions assertThat(ExcelFile actual) {
        return ExcelFileAssertions.assertThat(actual);
    }

    public static IssueAssertions assertThat(Issue actual) {
        return IssueAssertions.assertThat(actual);
    }

    public static OfferAssertions assertThat(Offer actual) {
        return OfferAssertions.assertThat(actual);
    }

    public static ListGeneralOfferAssertions assertThat(List<? extends Offer> actual) {
        return ListGeneralOfferAssertions.assertThat(actual);
    }

    public static IterableGeneralOffersAssert assertThat(Iterable<? extends Offer> actual) {
        return IterableGeneralOffersAssert.assertThat(actual);
    }

    public static void assertSoftly(Consumer<MbocSoftAssertions> softly) {
        MbocSoftAssertions assertions = new MbocSoftAssertions();
        softly.accept(assertions);
        assertions.assertAll();
    }

    public static class MbocSoftAssertions extends SoftAssertions {

        public ExcelFileAssertions assertThat(ExcelFile actual) {
            return proxy(ExcelFileAssertions.class, ExcelFile.class, actual);
        }

        public IssueAssertions assertThat(Issue actual) {
            return proxy(IssueAssertions.class, Issue.class, actual);
        }

        public OfferAssertions assertThat(Offer actual) {
            return proxy(OfferAssertions.class, Offer.class, actual);
        }
    }
}
