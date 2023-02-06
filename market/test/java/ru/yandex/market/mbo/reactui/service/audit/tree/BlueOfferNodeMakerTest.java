package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.BillingPricesRegistry;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.MockUtils;
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.BlueOfferNodeMaker;
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.MatchingOfferContext;
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.OfferNodeContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BlueOfferNodeMakerTest {

    private static final long INSPECTOR_UID = 1L;
    private static final long OPERATOR_UID = 1L;
    private static final Long OFFER_ID = 1L;
    private static final String OFFER_TITLE = "offer1";
    private static final YangLogStorage.MappingStatistic INSPECTOR_STATISTIC =
            YangLogStorage.MappingStatistic.newBuilder()
                    .setOfferId(OFFER_ID)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED)
                    .setComment(YangLogStorage.Comment.newBuilder()
                            .setType("type1")
                            .addItems("name1")
                        .build())
                    .build();
    private static final YangLogStorage.MappingStatistic OPERATOR_STATISTIC =
            YangLogStorage.MappingStatistic.newBuilder()
                    .setOfferId(OFFER_ID)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED)
                    .setComment(YangLogStorage.Comment.newBuilder()
                            .setType("type1")
                            .addItems("name1")
                            .build())
                    .build();
    private static final ModelStorage.Model MODEL1 = ModelStorage.Model.newBuilder()
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                    .setValue("model1")
                    .setIsoCode(Language.RUSSIAN.getIsoCode())
                    .build())
            .build();
    private static final ModelStorage.Model MODEL2 = ModelStorage.Model.newBuilder()
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                    .setValue("model2")
                    .setIsoCode(Language.RUSSIAN.getIsoCode())
                    .build())
            .build();

    private BillingPricesRegistry pricesRegistry = MockUtils.mockBillingPricesRegistry();

    @Test
    public void blueOfferActionNodeMakerTest() {
        OfferNodeContext<Long, YangLogStorage.MappingStatistic> context = new OfferNodeContext<>(
                INSPECTOR_UID,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                INSPECTOR_STATISTIC,
                pricesRegistry,
                new MatchingOfferContext(MODEL1, MODEL2)
        );

        AuditNode result = new BlueOfferNodeMaker(new HashMap<>()).apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeBlueOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name(),
                ColumnDefinition.OPERATOR_ERROR.name(),
                ColumnDefinition.INSPECTOR_ACTION.name(),
                ColumnDefinition.INSPECTOR_CHANGES.name(),
                ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void withoutInspector() {
        OfferNodeContext<Long, YangLogStorage.MappingStatistic> context = new OfferNodeContext<>(
                null,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                null,
                pricesRegistry,
                new MatchingOfferContext(MODEL1, null)
        );

        AuditNode result = new BlueOfferNodeMaker(new HashMap<>()).apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeBlueOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name()
        );
    }

    @Test
    public void inspectorChecked() {
        OfferNodeContext<Long, YangLogStorage.MappingStatistic> context = new OfferNodeContext<>(
                INSPECTOR_UID,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                null,
                pricesRegistry,
                new MatchingOfferContext(MODEL1, null)
        );

        AuditNode result = new BlueOfferNodeMaker(new HashMap<>()).apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeBlueOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name(),
                ColumnDefinition.INSPECTOR_PRICE.name(),
                ColumnDefinition.INSPECTOR_ACTION.name()
        );
    }

    @Test
    public void withoutOperator() {
        OfferNodeContext<Long, YangLogStorage.MappingStatistic> context = new OfferNodeContext<>(
                INSPECTOR_UID,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                null,
                INSPECTOR_STATISTIC,
                pricesRegistry,
                new MatchingOfferContext(null, MODEL1)
        );

        AuditNode result = new BlueOfferNodeMaker(new HashMap<>()).apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeBlueOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    private List<BaseComponent> makeBlueOfferActionNodeTitle() {
        return Arrays.asList(
                new Text("Оффер"),
                new Text(String.valueOf(OFFER_ID)),
                new Text(OFFER_TITLE)
        );
    }
}
