package ru.yandex.market.psku.postprocessor.bazinga.errremove;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferTechInfo;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.WrongErrorRemovalDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.daos.WrongErrorRemovalStatsDao;
import ru.yandex.market.psku.postprocessor.common.service.DataCampService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RemoveWrongErrorsFromOffersTaskTest extends BaseDBTest {
    @Mock
    DataCampService dataCampService;
    @Mock
    JdbcTemplate yqlJdbcTemplate;
    @Mock
    Yt yt;
    @Autowired
    WrongErrorRemovalDao wrongErrorRemovalDao;
    @Autowired
    WrongErrorRemovalStatsDao wrongErrorRemovalStatsDao;


    RemoveWrongErrorsFromOffersTask task;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(yt.cypress()).thenReturn(Mockito.mock(Cypress.class));
        when(yt.cypress().list(Mockito.any())).thenReturn(new ArrayListF<>());
    }

    @Test
    public void testJudgeForbidErrorsRemoval() {
        int businessId = 42;
        String shopSKu = "MOF";
        int paramId = 24;

        DataCampUnitedOffer.UnitedOffer offer = createOffer(businessId, shopSKu, List.of(
                Pair.of("paramId", Integer.toString(paramId))), true);
        mockDataCampService(offer);

        initJudgeForbid();

        mockYqlJdbcTemplate(List.of(new RemoveWrongErrorsFromOffersTask.YqlResultRecord(businessId, shopSKu, paramId)));

        task.execute(null);
        List<DataCampUnitedOffer.UnitedOffer> collect = dataCampService.getOffers(List.of(Pair.of(businessId,
                shopSKu))).collect(Collectors.toList());

        assertThat(collect).hasSize(1);

        DataCampUnitedOffer.UnitedOffer unitedOffer = collect.get(0);

        List<DataCampExplanation.Explanation> messagesList =
                unitedOffer.getBasic().getResolution().getBySource(0).getVerdict(0).getResults(0).getMessagesList();

        assertThat(messagesList).isEmpty();
    }

    @Test
    public void testRemovedFromOperatorsCard() {
        int businessId = 42;
        String shopSKu = "MOF";
        int paramId = 24;

        DataCampUnitedOffer.UnitedOffer offer = createOffer(businessId, shopSKu, List.of(
                Pair.of("paramId", Integer.toString(paramId))), true);
        mockDataCampService(offer);

        initRemovedFromOperatorsCard();

        mockYqlJdbcTemplate(List.of(new RemoveWrongErrorsFromOffersTask.YqlResultRecord(businessId, shopSKu, paramId)));

        task.execute(null);
        List<DataCampUnitedOffer.UnitedOffer> collect = dataCampService.getOffers(List.of(Pair.of(businessId,
                shopSKu))).collect(Collectors.toList());

        assertThat(collect).hasSize(1);

        DataCampUnitedOffer.UnitedOffer unitedOffer = collect.get(0);

        List<DataCampExplanation.Explanation> messagesList =
                unitedOffer.getBasic().getResolution().getBySource(0).getVerdict(0).getResults(0).getMessagesList();

        assertThat(messagesList).isEmpty();
    }

    @Test
    public void finishAfterFail() {
        int businessId = 42;
        String shopSKu = "MOF";
        int paramId = 24;

        DataCampUnitedOffer.UnitedOffer offer = createOffer(businessId, shopSKu, List.of(
                Pair.of("paramId", Integer.toString(paramId))), true);
        mockDataCampService(offer);

        initRemovedFromOperatorsCard();

        when(yqlJdbcTemplate.query(Mockito.any(String.class), Mockito.any(RowMapper.class)))
                .thenThrow(new RuntimeException("YQL is sad :("))
                .thenReturn(List.of(new RemoveWrongErrorsFromOffersTask.YqlResultRecord(businessId, shopSKu, paramId)))
                .thenReturn(Collections.emptyList());

        try {
            task.execute(null);
        } catch (Throwable t) {
            task.execute(null);
        }
        List<DataCampUnitedOffer.UnitedOffer> collect = dataCampService.getOffers(List.of(Pair.of(businessId,
                shopSKu))).collect(Collectors.toList());

        assertThat(collect).hasSize(1);

        DataCampUnitedOffer.UnitedOffer unitedOffer = collect.get(0);

        List<DataCampExplanation.Explanation> messagesList =
                unitedOffer.getBasic().getResolution().getBySource(0).getVerdict(0).getResults(0).getMessagesList();

        assertThat(messagesList).isEmpty();
    }

    @Test
    public void shouldNotRemoveOkError() {
        int businessId = 42;
        String shopSKu = "MOF";
        int paramId = 24;

        DataCampUnitedOffer.UnitedOffer offer = createOffer(businessId, shopSKu, List.of(
                Pair.of("paramId", Integer.toString(paramId))), true);
        mockDataCampService(offer);

        initJudgeForbid();

        mockYqlJdbcTemplate(List.of(new RemoveWrongErrorsFromOffersTask.YqlResultRecord(businessId, shopSKu, 48)));

        task.execute(null);
        List<DataCampUnitedOffer.UnitedOffer> collect = dataCampService.getOffers(List.of(Pair.of(businessId,
                shopSKu))).collect(Collectors.toList());

        assertThat(collect).hasSize(1);

        DataCampUnitedOffer.UnitedOffer unitedOffer = collect.get(0);

        List<DataCampExplanation.Explanation> messagesList =
                unitedOffer.getBasic().getResolution().getBySource(0).getVerdict(0).getResults(0).getMessagesList();

        assertThat(messagesList).hasSize(1);
    }

    @Test
    public void shouldNotRemoveNotGGVerdicts() {
        int businessId = 42;
        String shopSKu = "MOF";
        int paramId = 24;

        DataCampUnitedOffer.UnitedOffer offer = createOffer(businessId, shopSKu, List.of(
                Pair.of("paramId", Integer.toString(paramId))), false);
        mockDataCampService(offer);

        task = new RemoveJudgeForbidErrorsFromOffersTask(
                yqlJdbcTemplate,
                yt,
                wrongErrorRemovalDao,
                "development",
                dataCampService,
                wrongErrorRemovalStatsDao);

        mockYqlJdbcTemplate(List.of(new RemoveWrongErrorsFromOffersTask.YqlResultRecord(businessId, shopSKu, 24)));

        task.execute(null);
        List<DataCampUnitedOffer.UnitedOffer> collect = dataCampService.getOffers(List.of(Pair.of(businessId,
                shopSKu))).collect(Collectors.toList());

        assertThat(collect).hasSize(1);

        DataCampUnitedOffer.UnitedOffer unitedOffer = collect.get(0);

        List<DataCampExplanation.Explanation> messagesList =
                unitedOffer.getBasic().getResolution().getBySource(0).getVerdict(0).getResults(0).getMessagesList();

        assertThat(messagesList).hasSize(1);
    }


    private void mockDataCampService(DataCampUnitedOffer.UnitedOffer offer) {
        dataCampService = new DataCampServiceMock(List.of(offer));
    }

    private void mockYqlJdbcTemplate(List<RemoveWrongErrorsFromOffersTask.YqlResultRecord> records) {
        when(yqlJdbcTemplate.query(Mockito.any(String.class), Mockito.any(RowMapper.class)))
                .thenReturn(records)
                .thenReturn(Collections.emptyList());
    }

    private DataCampUnitedOffer.UnitedOffer createOffer(int businessId, String shopSku,
                                                        List<Pair<String, String>> params, boolean ggVerdicts) {
        DataCampExplanation.Explanation explanation = DataCampExplanation.Explanation.newBuilder()
                .addAllParams(params.stream()
                        .map(param -> DataCampExplanation.Explanation.Param.newBuilder()
                                .setName(param.getFirst())
                                .setValue(param.getSecond())
                                .build()
                        )
                        .collect(Collectors.toList())
                ).build();

        DataCampValidationResult.ValidationResult validationResult =
                DataCampValidationResult.ValidationResult.newBuilder()
                        .addMessages(explanation)
                        .build();

        DataCampResolution.Verdict verdict = DataCampResolution.Verdict.newBuilder()
                .addResults(validationResult)
                .build();

        DataCampResolution.Verdicts verdicts = DataCampResolution.Verdicts.newBuilder()
                .addVerdict(verdict)
                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                        .setSource(ggVerdicts ? DataCampOfferMeta.DataSource.MARKET_GUTGIN :
                                DataCampOfferMeta.DataSource.MARKET_MBO)
                        .build())
                .build();

        DataCampResolution.Resolution resolution = DataCampResolution.Resolution.newBuilder()
                .addBySource(verdicts)
                .build();

        return DataCampUnitedOffer.UnitedOffer
                .newBuilder()
                .setBasic(
                        DataCampOffer.Offer.newBuilder()
                                .setResolution(resolution)
                                .setTechInfo(
                                        DataCampOfferTechInfo.OfferTechInfo.newBuilder()
                                                .setLastParsing(
                                                        DataCampOfferTechInfo.ParserTrace.newBuilder()
                                                                .setEndParsing(Timestamp.newBuilder()
                                                                        .setSeconds(Instant.now().minus(Duration.ofDays(1)).getEpochSecond())
                                                                        .build())
                                                                .build()
                                                )
                                                .build()
                                )
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setBusinessId(businessId)
                                        .setOfferId(shopSku)
                                        .build())
                                .build()
                )
                .build();
    }

    public void initJudgeForbid() {
        task = new RemoveJudgeForbidErrorsFromOffersTask(
                yqlJdbcTemplate,
                yt,
                wrongErrorRemovalDao,
                "development",
                dataCampService,
                wrongErrorRemovalStatsDao);
    }

    public void initRemovedFromOperatorsCard() {
        task = new RemoveRemovedFromOperatorCardErrorsFromOffersTask(
                yqlJdbcTemplate,
                yt,
                wrongErrorRemovalDao,
                "development",
                dataCampService,
                wrongErrorRemovalStatsDao);
    }
}
