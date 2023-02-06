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
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.MatchingOfferContext;
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.OfferNodeContext;
import ru.yandex.market.mbo.reactui.service.audit.tree.offers.WhiteOfferNodeMaker;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WhiteOfferNodeMakerTest {

    private static final long INSPECTOR_UID = 1L;
    private static final long OPERATOR_UID = 1L;
    private static final String OFFER_ID = "offer1";
    private static final String OFFER_TITLE = "offer1";
    private static final YangLogStorage.MatchingStatistic INSPECTOR_STATISTIC =
            YangLogStorage.MatchingStatistic.newBuilder()
                .setOfferId(OFFER_ID)
                .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
            .build();
    private static final YangLogStorage.MatchingStatistic OPERATOR_STATISTIC =
            YangLogStorage.MatchingStatistic.newBuilder()
                .setOfferId(OFFER_ID)
                .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED)
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
    public void whiteOfferActionNodeMakerTest() {
        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
            INSPECTOR_UID,
            OPERATOR_UID,
            OFFER_ID,
            OFFER_TITLE,
            OPERATOR_STATISTIC,
            INSPECTOR_STATISTIC,
            pricesRegistry,
            new MatchingOfferContext(MODEL1, MODEL2)
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

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
        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                null,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                null,
                pricesRegistry,
                new MatchingOfferContext(MODEL1, null)
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name()
        );
    }

    @Test
    public void inspectorChecked() {
        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                INSPECTOR_UID,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                null,
                pricesRegistry,
                new MatchingOfferContext(MODEL1, null)
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name(),
                ColumnDefinition.INSPECTOR_ACTION.name(),
                ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void withoutMatching() {
        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                INSPECTOR_UID,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                INSPECTOR_STATISTIC,
                pricesRegistry,
                null
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

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
    public void withoutOperator() {
        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                INSPECTOR_UID,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                null,
                INSPECTOR_STATISTIC,
                pricesRegistry,
                new MatchingOfferContext(null, MODEL1)
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.INSPECTOR_ACTION.name(),
                ColumnDefinition.INSPECTOR_CHANGES.name(),
                ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void testStatus() {
        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                null,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                OPERATOR_STATISTIC,
                null,
                pricesRegistry,
                null
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name()
        );
        List<BaseComponent> operatorAction = result.getData().get(ColumnDefinition.OPERATOR_CHANGES.name());
        assertEquals(1, operatorAction.size());
        assertEquals("Скутчинг", ((Text) operatorAction.get(0)).getValue());
    }

    @Test
    public void testCannotBeImproveStatusWithModel() {
       YangLogStorage.MatchingStatistic statistic =  OPERATOR_STATISTIC.toBuilder()
               .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
               .build();

        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                null,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                statistic,
                null,
                pricesRegistry,
                new MatchingOfferContext(MODEL1, null)
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name()
        );
        List<BaseComponent> operatorAction = result.getData().get(ColumnDefinition.OPERATOR_CHANGES.name());
        assertEquals(2, operatorAction.size());
        assertEquals("Ручная привязка", ((Text) operatorAction.get(0)).getValue());
        assertEquals("MSKU " + MODEL1.getId() + " " + MODEL1.getTitlesList().get(0).getValue(),
                ((Text) operatorAction.get(1)).getValue());
    }

    @Test
    public void testCannotBeImproveStatusWithoutModel() {
        YangLogStorage.MatchingStatistic statistic =  OPERATOR_STATISTIC.toBuilder()
                .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
                .build();

        OfferNodeContext<String, YangLogStorage.MatchingStatistic> context = new OfferNodeContext<>(
                null,
                OPERATOR_UID,
                OFFER_ID,
                OFFER_TITLE,
                statistic,
                null,
                pricesRegistry,
                null
        );

        AuditNode result = new WhiteOfferNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeWhiteOfferActionNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
                ColumnDefinition.OPERATOR_ACTION.name(),
                ColumnDefinition.OPERATOR_CHANGES.name(),
                ColumnDefinition.OPERATOR_PRICE.name()
        );
        List<BaseComponent> operatorAction = result.getData().get(ColumnDefinition.OPERATOR_CHANGES.name());
        assertEquals(1, operatorAction.size());
        assertEquals("Нельзя улучшить", ((Text) operatorAction.get(0)).getValue());
    }

    private List<BaseComponent> makeWhiteOfferActionNodeTitle() {
        return Arrays.asList(
                new Text("Оффер"),
                new Text(String.valueOf(OFFER_ID)),
                new Text(OFFER_TITLE)
        );
    }
}
