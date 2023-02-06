package ru.yandex.direct.logicprocessor.processors.recomtracer.cancellers.pages;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationKey;
import ru.yandex.direct.ess.logicobjects.recomtracer.AdditionalColumns;
import ru.yandex.direct.logicprocessor.processors.recomtracer.RecommendationKeyWithAdditionalColumns;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

class RemovePagesFromBlackListCancellerTest {

    @Test
    void recommendationToCancelTest() {
        RemovePagesFromBlackListCanceller removePagesFromBlackListCanceller = new RemovePagesFromBlackListCanceller();
        RecommendationKey recommendationKeyToCancel = new RecommendationKey().withUserKey1("www.100molitv.ru");
        RecommendationKey recommendationKeyNoCancel = new RecommendationKey().withUserKey1("www.fishki.net");
        AdditionalColumns additionalColumns = new AdditionalColumns();
        additionalColumns.add(CAMPAIGNS.DONT_SHOW, "www.fishki.net,www.afisha.ru");

        AdditionalColumns additionalColumnsCancelAll = new AdditionalColumns();
        additionalColumnsCancelAll.add(CAMPAIGNS.DONT_SHOW, "");

        RecommendationKeyWithAdditionalColumns toCancel =
                new RecommendationKeyWithAdditionalColumns(recommendationKeyToCancel, additionalColumns);

        RecommendationKeyWithAdditionalColumns noCancel =
                new RecommendationKeyWithAdditionalColumns(recommendationKeyNoCancel, additionalColumns);

        RecommendationKeyWithAdditionalColumns allCancel1 =
                new RecommendationKeyWithAdditionalColumns(recommendationKeyToCancel, additionalColumnsCancelAll);

        RecommendationKeyWithAdditionalColumns allCancel2 =
                new RecommendationKeyWithAdditionalColumns(recommendationKeyNoCancel, additionalColumnsCancelAll);

        assertThat(removePagesFromBlackListCanceller.recommendationsToCancel(toCancel)).isTrue();
        assertThat(removePagesFromBlackListCanceller.recommendationsToCancel(noCancel)).isFalse();
        assertThat(removePagesFromBlackListCanceller.recommendationsToCancel(allCancel1)).isTrue();
        assertThat(removePagesFromBlackListCanceller.recommendationsToCancel(allCancel2)).isTrue();
    }

    @Test
    void recommendationToCancelTest_NotDontShowObject() {
        RemovePagesFromBlackListCanceller removePagesFromBlackListCanceller = new RemovePagesFromBlackListCanceller();
        RecommendationKey recommendationKey = new RecommendationKey().withUserKey1("www.100molitv.ru");
        AdditionalColumns additionalColumns = new AdditionalColumns();

        RecommendationKeyWithAdditionalColumns recommendationKeyWithAdditionalColumns =
                new RecommendationKeyWithAdditionalColumns(recommendationKey, additionalColumns);

        assertThat(removePagesFromBlackListCanceller.recommendationsToCancel(recommendationKeyWithAdditionalColumns)).isTrue();
    }
}
