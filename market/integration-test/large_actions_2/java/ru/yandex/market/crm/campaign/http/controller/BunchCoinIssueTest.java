package ru.yandex.market.crm.campaign.http.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.RandomUtils;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.object.YTreeSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializerFactory;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.Action;
import ru.yandex.market.crm.campaign.domain.actions.ActionStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.LoyaltyTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.external.loyalty.BunchCheckResponse;
import ru.yandex.market.crm.external.loyalty.CoinBunchSaveRequest;
import ru.yandex.market.crm.external.loyalty.LoyaltyCoinType;
import ru.yandex.market.crm.external.loyalty.LoyaltyUtils;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.loyalty.Coin;
import ru.yandex.market.crm.mapreduce.domain.loyalty.LoyaltyCoin;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;

/**
 * @author zloddey
 */
public class BunchCoinIssueTest extends AbstractControllerLargeTest {

    private IssueCoinsStep step;
    private Action action;
    private YPath generatedCoinsAuthTable;
    private YPath generatedCoinsNoAuthTable;

    private static final YTreeSerializer<Coin> COIN_SERIALIZER = YTreeObjectSerializerFactory.forClass(Coin.class);

    private static final String EMAIL_COMPANY = "EMAIL_COMPANY";

    // Идентификатор должен быть новым для каждого теста
    private final long promoId = RandomUtils.nextIntInRange(0, 100_000_000);

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private YtSchemaTestHelper schemaTestHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private YtClient ytClient;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private LoyaltyTestHelper loyaltyTestHelper;

    @BeforeEach
    public void setUp() {
        schemaTestHelper.preparePlusDataTable();
        schemaTestHelper.prepareAccessDataTables();
        schemaTestHelper.prepareMetrikaAppFactsTable();
        schemaTestHelper.preparePassportProfilesTable();
        schemaTestHelper.prepareEmailOwnershipFactsTable();
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.PUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.YUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCampaignDir();
        schemaTestHelper.prepareSubscriptionFactsTable();
    }

    /**
     * Тестирование выдачи монеток для пользователей, известных по PIUD
     */
    @Test
    public void testIssueCoinsForPUIDStep() throws Exception {
        prepareActionAndStep();
        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.PUID, "1"),
                outputRow(UidType.PUID, "2"),
                outputRow(UidType.PUID, "3"),
                outputRow(UidType.PUID, "4"),
                outputRow(UidType.PUID, "5")
        );
        prepareSegmentationResult(coinsToIssue);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsBunch("982734",
                this.bunchAuthRequestValidator(saveRequestCounter, 5),
                bunchId -> {
                    statusRequestCounter.incrementAndGet();
                    issueCoins(generatedCoinsAuthTable, coinsToIssue);
                    var response = new BunchCheckResponse();
                    response.setStatus("OK");
                    response.setProcessedCount(5);
                    response.setErrors(Collections.emptyMap());
                    return response;
                }
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveRequestCounter.get());
        Assertions.assertEquals(1, statusRequestCounter.get());

        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        Assertions.assertEquals(Integer.valueOf(5), status.getPlannedCount());
        Assertions.assertEquals(Integer.valueOf(5), status.getIssuedCount());

        List<StepOutputRow> result = readResults(step);
        Assertions.assertEquals(5, result.size());
        assertCoinVarFilled(result, 1);
    }

    /**
     * Тестирование выдачи монеток для пользователей, известных по YUID
     */
    @Test
    public void testIssueCoinsForYUIDStep() throws Exception {
        prepareActionAndStep();
        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.YUID, "11"),
                outputRow(UidType.YUID, "13"),
                outputRow(UidType.YUID, "14"),
                outputRow(UidType.YUID, "12"),
                outputRow(UidType.YUID, "15")
        );
        prepareSegmentationResult(coinsToIssue);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsBunch("87967234",
                this.bunchNotAuthRequestValidator(saveRequestCounter, 5),
                bunchId -> {
                    statusRequestCounter.incrementAndGet();
                    issueCoins(generatedCoinsNoAuthTable, coinsToIssue);
                    var response = new BunchCheckResponse();
                    response.setStatus("OK");
                    response.setProcessedCount(5);
                    response.setErrors(Collections.emptyMap());
                    return response;
                }
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveRequestCounter.get());
        Assertions.assertEquals(1, statusRequestCounter.get());

        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        Assertions.assertEquals(Integer.valueOf(5), status.getPlannedCount());
        Assertions.assertEquals(Integer.valueOf(5), status.getPlannedCount(LoyaltyCoinType.NO_AUTH.name()));
        Assertions.assertEquals(Integer.valueOf(5), status.getIssuedCount());

        List<StepOutputRow> result = readResults(step);
        Assertions.assertEquals(5, result.size());
        assertCoinVarFilled(result, 1);
    }

    /**
     * Тестирование выдачи монеток для пользователей, известных по PUID, с ограничением по размеру акции
     */
    @Test
    public void testIssueCoinsForPUIDWithActionSizeLimit() throws Exception {
        final int actionSizeLimit = 1;
        prepareActionAndStep(actionSizeLimit);

        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.PUID, "1"),
                outputRow(UidType.PUID, "2"),
                outputRow(UidType.PUID, "3"),
                outputRow(UidType.PUID, "4"),
                outputRow(UidType.PUID, "5")
        );
        prepareSegmentationResult(coinsToIssue);
        List<StepOutputRow> coinsToGenerate = coinsToIssue.subList(0, actionSizeLimit);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsBunch("12378634",
                this.bunchAuthRequestValidator(saveRequestCounter, actionSizeLimit),
                bunchId -> {
                    statusRequestCounter.incrementAndGet();
                    issueCoins(generatedCoinsAuthTable, coinsToGenerate);
                    var response = new BunchCheckResponse();
                    response.setStatus("OK");
                    response.setProcessedCount(1);
                    response.setErrors(Collections.emptyMap());
                    return response;
                }
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveRequestCounter.get());
        Assertions.assertEquals(1, statusRequestCounter.get());

        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        Assertions.assertEquals(Integer.valueOf(actionSizeLimit), status.getPlannedCount());
        Assertions.assertEquals(Integer.valueOf(actionSizeLimit), status.getPlannedCount(LoyaltyCoinType.AUTH.name()));
        Assertions.assertEquals(Integer.valueOf(actionSizeLimit), status.getIssuedCount());

        List<StepOutputRow> result = readResults(step);
        Assertions.assertEquals(actionSizeLimit, result.size());
        assertCoinVarFilled(result, 1);
    }

    /**
     * Тестирование выдачи монеток для пользователей, известных по YUID, с ограничением по размеру акции
     */
    @Test
    public void testIssueCoinsForYUIDWithActionSizeLimit() throws Exception {
        final int actionSizeLimit = 1;
        prepareActionAndStep(actionSizeLimit);

        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.YUID, "11"),
                outputRow(UidType.YUID, "13"),
                outputRow(UidType.YUID, "14"),
                outputRow(UidType.YUID, "12"),
                outputRow(UidType.YUID, "15")
        );
        prepareSegmentationResult(coinsToIssue);
        List<StepOutputRow> coinsToGenerate = coinsToIssue.subList(0, actionSizeLimit);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsBunch("78634",
                this.bunchNotAuthRequestValidator(saveRequestCounter, actionSizeLimit),
                bunchId -> {
                    statusRequestCounter.incrementAndGet();
                    issueCoins(generatedCoinsNoAuthTable, coinsToGenerate);
                    var response = new BunchCheckResponse();
                    response.setStatus("OK");
                    response.setProcessedCount(1);
                    response.setErrors(Collections.emptyMap());
                    return response;
                }
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveRequestCounter.get());
        Assertions.assertEquals(1, statusRequestCounter.get());

        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        Assertions.assertEquals(Integer.valueOf(actionSizeLimit), status.getPlannedCount());
        Assertions.assertEquals(Integer.valueOf(actionSizeLimit),
                status.getPlannedCount(LoyaltyCoinType.NO_AUTH.name()));
        Assertions.assertEquals(Integer.valueOf(actionSizeLimit), status.getIssuedCount());

        List<StepOutputRow> result = readResults(step);
        Assertions.assertEquals(actionSizeLimit, result.size());
        assertCoinVarFilled(result, 1);
    }

    /**
     * Тестирование выдачи монеток для смеси аутентифицированных и не-аутф пользователей
     */
    @Test
    public void testIssueCoinsForPUIDAndYUIDStep() throws Exception {
        prepareActionAndStep();
        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.PUID, "21"),
                outputRow(UidType.PUID, "22"),
                outputRow(UidType.YUID, "23"),
                outputRow(UidType.YUID, "24"),
                outputRow(UidType.PUID, "25")
        );
        var issuedAuthCoins = coinsToIssue.stream()
                .filter(row -> UidType.PUID.equals(row.getIdType()))
                .collect(Collectors.toList());
        var issuedNonAuthCoins = coinsToIssue.stream()
                .filter(row -> UidType.YUID.equals(row.getIdType()))
                .collect(Collectors.toList());

        prepareSegmentationResult(coinsToIssue);

        AtomicInteger saveAuthRequestCounter = new AtomicInteger(0);
        AtomicInteger saveNonAuthRequestCounter = new AtomicInteger(0);
        AtomicInteger statusAuthRequestCounter = new AtomicInteger(0);
        AtomicInteger statusNonAuthRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsSeveralBunches(
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8881",
                        this.bunchAuthRequestValidator(saveAuthRequestCounter, issuedAuthCoins.size()),
                        bunchId -> {
                            statusAuthRequestCounter.incrementAndGet();
                            issueCoins(generatedCoinsAuthTable, issuedAuthCoins);
                            var response = new BunchCheckResponse();
                            response.setStatus("OK");
                            response.setProcessedCount(issuedAuthCoins.size());
                            response.setErrors(Collections.emptyMap());
                            return response;
                        }
                ),
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8882",
                        this.bunchNotAuthRequestValidator(saveNonAuthRequestCounter, issuedNonAuthCoins.size()),
                        bunchId -> {
                            statusNonAuthRequestCounter.incrementAndGet();
                            issueCoins(generatedCoinsNoAuthTable, issuedNonAuthCoins);
                            var response = new BunchCheckResponse();
                            response.setStatus("OK");
                            response.setProcessedCount(issuedNonAuthCoins.size());
                            response.setErrors(Collections.emptyMap());
                            return response;
                        }
                )
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveAuthRequestCounter.get());
        Assertions.assertEquals(1, saveNonAuthRequestCounter.get());
        Assertions.assertEquals(1, statusAuthRequestCounter.get());
        Assertions.assertEquals(1, statusNonAuthRequestCounter.get());

        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        Assertions.assertEquals(Integer.valueOf(3), status.getPlannedCount(LoyaltyCoinType.AUTH.name()));
        Assertions.assertEquals(Integer.valueOf(2), status.getPlannedCount(LoyaltyCoinType.NO_AUTH.name()));
        Assertions.assertEquals(Integer.valueOf(5), status.getPlannedCount());
        Assertions.assertEquals(Integer.valueOf(5), status.getIssuedCount());

        List<StepOutputRow> result = readResults(step);
        Assertions.assertEquals(5, result.size());
        assertCoinVarFilled(result, 1);
    }

    /**
     * Пока сервис loyalty не сообщит о завершении, регулярно запрашиваем состояние
     */
    @Test
    public void testRequestStatusSeveralTimes() throws Exception {
        prepareActionAndStep();
        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.PUID, "1"),
                outputRow(UidType.PUID, "2"),
                outputRow(UidType.PUID, "3"),
                outputRow(UidType.PUID, "4"),
                outputRow(UidType.PUID, "5")
        );
        prepareSegmentationResult(coinsToIssue);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);

        List<BunchCheckResponse> statusResponses = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            var response = new BunchCheckResponse();
            response.setStatus("OK");
            response.setProcessedCount(i);
            response.setErrors(Collections.emptyMap());
            statusResponses.add(response);
        }

        loyaltyTestHelper.issueCoinsBunch("92432",
                this.bunchAuthRequestValidator(saveRequestCounter, 5),
                bunchId -> {
                    statusRequestCounter.incrementAndGet();
                    issueCoins(generatedCoinsAuthTable, coinsToIssue);
                    return statusResponses.remove(0);
                }
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveRequestCounter.get());
        Assertions.assertEquals(5, statusRequestCounter.get());
    }

    /**
     * Когда акция состоит из нескольких шагов, то каждый шаг должен добавлять к своим монетам
     * все монеты, которые были выданы PUID-идентификатору на предыдущих шагах акции.
     */
    @Test
    public void mergeResultsForSeveralStepsPUID() throws Exception {
        step = ActionTestHelper.issueCoins(promoId);
        IssueCoinsStep step1 = ActionTestHelper.issueCoins(promoId, 2);
        IssueCoinsStep step2 = ActionTestHelper.issueCoins(promoId, 0);
        action = prepareAction(step, step1, step2);

        generatedCoinsAuthTable = coinTable("output", tableName(LoyaltyCoinType.AUTH));
        YPath generatedCoinsAuthTable1 = coinTable("output", tableName(LoyaltyCoinType.AUTH, step1));

        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.PUID, "91"),
                outputRow(UidType.PUID, "93"),
                outputRow(UidType.PUID, "90")
        );
        List<StepOutputRow> coinsToIssue1 = coinsToIssue.subList(0, step1.getSizeLimit());

        prepareSegmentationResult(coinsToIssue);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsSeveralBunches(
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8881",
                        this.bunchAuthRequestValidator(saveRequestCounter, coinsToIssue.size()),
                        bunchId -> {
                            statusRequestCounter.incrementAndGet();
                            issueCoins(generatedCoinsAuthTable, coinsToIssue);
                            var response = new BunchCheckResponse();
                            response.setStatus("OK");
                            response.setProcessedCount(coinsToIssue.size());
                            response.setErrors(Collections.emptyMap());
                            return response;
                        }
                ),
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8882",
                        this.bunchAuthRequestValidator(step1, saveRequestCounter, coinsToIssue1.size()),
                        bunchId -> {
                            statusRequestCounter.incrementAndGet();
                            issueCoins(generatedCoinsAuthTable1, coinsToIssue1);
                            var response = new BunchCheckResponse();
                            response.setStatus("OK");
                            response.setProcessedCount(coinsToIssue1.size());
                            response.setErrors(Collections.emptyMap());
                            return response;
                        }
                ),
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8883",
                        this.bunchAuthRequestValidator(step2, saveRequestCounter, 0),
                        bunchId -> null
                )
        );

        requestLaunch();
        waitForStepStatus(step2.getId(), this::stepIsFinished);

        Assertions.assertEquals(2, saveRequestCounter.get());
        Assertions.assertEquals(2, statusRequestCounter.get());

        List<StepOutputRow> result = readResults(step2);
        Assertions.assertEquals(3, result.size());

        Set<String> step1Ids = coinsToIssue1.stream()
                .map(StepOutputRow::getIdValue)
                .collect(Collectors.toUnmodifiableSet());

        assertCoinVarFilled(
                result.stream()
                        .filter(row -> step1Ids.contains(row.getIdValue()))
                        .collect(Collectors.toUnmodifiableList()),
                2
        );
        assertCoinVarFilled(
                result.stream()
                        .filter(row -> !step1Ids.contains(row.getIdValue()))
                        .collect(Collectors.toUnmodifiableList()),
                1
        );
    }

    /**
     * Когда акция состоит из нескольких шагов, то каждый шаг должен добавлять к своим монетам
     * все монеты, которые были выданы YUID-идентификатору на предыдущих шагах акции.
     */
    @Test
    public void mergeResultsForSeveralStepsYUID() throws Exception {
        step = ActionTestHelper.issueCoins(promoId);
        IssueCoinsStep step1 = ActionTestHelper.issueCoins(promoId, 2);
        IssueCoinsStep step2 = ActionTestHelper.issueCoins(promoId, 0);
        action = prepareAction(step, step1, step2);

        generatedCoinsNoAuthTable = coinTable("output", tableName(LoyaltyCoinType.NO_AUTH));
        YPath generatedCoinsNoAuthTable1 = coinTable("output", tableName(LoyaltyCoinType.NO_AUTH, step1));

        List<StepOutputRow> coinsToIssue = Arrays.asList(
                outputRow(UidType.YUID, "191"),
                outputRow(UidType.YUID, "193"),
                outputRow(UidType.YUID, "190")
        );
        List<StepOutputRow> coinsToIssue1 = coinsToIssue.subList(0, step1.getSizeLimit());

        prepareSegmentationResult(coinsToIssue);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCoinsSeveralBunches(
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8881",
                        this.bunchNotAuthRequestValidator(saveRequestCounter, coinsToIssue.size()),
                        bunchId -> {
                            statusRequestCounter.incrementAndGet();
                            issueCoins(generatedCoinsNoAuthTable, coinsToIssue);
                            var response = new BunchCheckResponse();
                            response.setStatus("OK");
                            response.setProcessedCount(coinsToIssue.size());
                            response.setErrors(Collections.emptyMap());
                            return response;
                        }
                ),
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8882",
                        this.bunchNotAuthRequestValidator(step1, saveRequestCounter, coinsToIssue1.size()),
                        bunchId -> {
                            statusRequestCounter.incrementAndGet();
                            issueCoins(generatedCoinsNoAuthTable1, coinsToIssue1);
                            var response = new BunchCheckResponse();
                            response.setStatus("OK");
                            response.setProcessedCount(coinsToIssue1.size());
                            response.setErrors(Collections.emptyMap());
                            return response;
                        }
                ),
                new LoyaltyTestHelper.IssueCoinsBunchData(
                        "8883",
                        this.bunchAuthRequestValidator(step2, saveRequestCounter, 0),
                        bunchId -> null
                )
        );

        requestLaunch();
        waitForStepStatus(step2.getId(), this::stepIsFinished);

        Assertions.assertEquals(2, saveRequestCounter.get());
        Assertions.assertEquals(2, statusRequestCounter.get());

        List<StepOutputRow> result = readResults(step2);
        Assertions.assertEquals(3, result.size());

        assertCoinVarFilled(result);

        AtomicInteger oneCoin = new AtomicInteger(0);
        AtomicInteger twoCoins = new AtomicInteger(0);
        result.stream()
                .map(row -> row.getData().getVars().get("COINS").asList().size())
                .forEach(coinsCount -> {
                    if (coinsCount == 1) {
                        oneCoin.incrementAndGet();
                    } else if (coinsCount == 2) {
                        twoCoins.incrementAndGet();
                    }
                });
        Assertions.assertEquals(1, oneCoin.get());
        Assertions.assertEquals(2, twoCoins.get());
    }

    private void prepareActionAndStep() {
        prepareActionAndStep(null);
    }

    private void prepareActionAndStep(Integer stepSizeLimit) {
        step = ActionTestHelper.issueCoins(promoId, stepSizeLimit);
        action = prepareAction(step);

        generatedCoinsAuthTable = coinTable("output", tableName(LoyaltyCoinType.AUTH));
        generatedCoinsNoAuthTable = coinTable("output", tableName(LoyaltyCoinType.NO_AUTH));
    }

    private String tableName(LoyaltyCoinType coinType) {
        return tableName(coinType, step);
    }

    private String tableName(LoyaltyCoinType coinType, IssueCoinsStep step) {
        return String.join("_", action.getId(), step.getId(), coinType.name());
    }

    private YPath coinTable(String dir, String name) {
        return ytFolders.getCoinRequestFolder().child(dir).child(name);
    }

    private Consumer<CoinBunchSaveRequest> bunchAuthRequestValidator(AtomicInteger counter, int saveCount) {
        return bunchSaveRequest -> {
            counter.incrementAndGet();
            Assertions.assertEquals(LoyaltyCoinType.AUTH, bunchSaveRequest.getType());
            Assertions.assertFalse(bunchSaveRequest.getUniqueKey().isEmpty());
            Assertions.assertEquals(tableName(LoyaltyCoinType.AUTH), bunchSaveRequest.getInput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.AUTH), bunchSaveRequest.getOutput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.AUTH) + "_errors", bunchSaveRequest.getErrorsOutput());
            Assertions.assertEquals(Long.valueOf(promoId), bunchSaveRequest.getPromoId());
            Assertions.assertEquals("", bunchSaveRequest.getPromoAlias());
            Assertions.assertEquals(Integer.valueOf(saveCount), bunchSaveRequest.getCount());
            Assertions.assertEquals(LoyaltyUtils.REASON_EMAIL_COMPANY, bunchSaveRequest.getReason());
            Assertions.assertEquals(LoyaltyUtils.OUTPUT_FORMAT_YT, bunchSaveRequest.getOutputFormat());
            Assertions.assertNull(bunchSaveRequest.getEmail());
        };
    }

    private Consumer<CoinBunchSaveRequest> bunchAuthRequestValidator(IssueCoinsStep step, AtomicInteger counter,
                                                                     int saveCount) {
        return bunchSaveRequest -> {
            counter.incrementAndGet();
            Assertions.assertEquals(LoyaltyCoinType.AUTH, bunchSaveRequest.getType());
            Assertions.assertFalse(bunchSaveRequest.getUniqueKey().isEmpty());
            Assertions.assertEquals(tableName(LoyaltyCoinType.AUTH, step), bunchSaveRequest.getInput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.AUTH, step), bunchSaveRequest.getOutput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.AUTH, step) + "_errors",
                    bunchSaveRequest.getErrorsOutput());
            Assertions.assertEquals(Long.valueOf(promoId), bunchSaveRequest.getPromoId());
            Assertions.assertEquals("", bunchSaveRequest.getPromoAlias());
            Assertions.assertEquals(Integer.valueOf(saveCount), bunchSaveRequest.getCount());
            Assertions.assertEquals(LoyaltyUtils.REASON_EMAIL_COMPANY, bunchSaveRequest.getReason());
            Assertions.assertEquals(LoyaltyUtils.OUTPUT_FORMAT_YT, bunchSaveRequest.getOutputFormat());
            Assertions.assertNull(bunchSaveRequest.getEmail());
        };
    }

    private Consumer<CoinBunchSaveRequest> bunchNotAuthRequestValidator(AtomicInteger counter, int saveCount) {
        return bunchSaveRequest -> {
            counter.incrementAndGet();
            Assertions.assertEquals(LoyaltyCoinType.NO_AUTH, bunchSaveRequest.getType());
            Assertions.assertFalse(bunchSaveRequest.getUniqueKey().isEmpty());
            // Если input для NO_AUTH не будет пустым, получим ошибку от loyalty
            Assertions.assertNull(bunchSaveRequest.getInput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.NO_AUTH), bunchSaveRequest.getOutput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.NO_AUTH) + "_errors", bunchSaveRequest.getErrorsOutput());
            Assertions.assertEquals(Long.valueOf(promoId), bunchSaveRequest.getPromoId());
            Assertions.assertEquals("", bunchSaveRequest.getPromoAlias());
            Assertions.assertEquals(Integer.valueOf(saveCount), bunchSaveRequest.getCount());
            Assertions.assertEquals(LoyaltyUtils.REASON_EMAIL_COMPANY, bunchSaveRequest.getReason());
            Assertions.assertEquals(LoyaltyUtils.OUTPUT_FORMAT_YT, bunchSaveRequest.getOutputFormat());
            Assertions.assertNull(bunchSaveRequest.getEmail());
        };
    }

    private Consumer<CoinBunchSaveRequest> bunchNotAuthRequestValidator(IssueCoinsStep step, AtomicInteger counter,
                                                                        int saveCount) {
        return bunchSaveRequest -> {
            counter.incrementAndGet();
            Assertions.assertEquals(LoyaltyCoinType.NO_AUTH, bunchSaveRequest.getType());
            Assertions.assertFalse(bunchSaveRequest.getUniqueKey().isEmpty());
            // Если input для NO_AUTH не будет пустым, получим ошибку от loyalty
            Assertions.assertNull(bunchSaveRequest.getInput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.NO_AUTH, step), bunchSaveRequest.getOutput());
            Assertions.assertEquals(tableName(LoyaltyCoinType.NO_AUTH, step) + "_errors",
                    bunchSaveRequest.getErrorsOutput());
            Assertions.assertEquals(Long.valueOf(promoId), bunchSaveRequest.getPromoId());
            Assertions.assertEquals("", bunchSaveRequest.getPromoAlias());
            Assertions.assertEquals(Integer.valueOf(saveCount), bunchSaveRequest.getCount());
            Assertions.assertEquals(LoyaltyUtils.REASON_EMAIL_COMPANY, bunchSaveRequest.getReason());
            Assertions.assertEquals(LoyaltyUtils.OUTPUT_FORMAT_YT, bunchSaveRequest.getOutputFormat());
            Assertions.assertNull(bunchSaveRequest.getEmail());
        };
    }

    private void issueCoins(YPath path, List<StepOutputRow> coins) {
        List<LoyaltyCoin> issuedCoins = coins.stream()
                .map(this::intoIssuedCoin)
                .collect(Collectors.toList());
        ytClient.write(path, LoyaltyCoin.class, issuedCoins);
    }

    private LoyaltyCoin intoIssuedCoin(StepOutputRow data) {
        boolean authenticated = UidType.PUID.equals(data.getIdType());
        LoyaltyCoin issuedCoin = new LoyaltyCoin();
        issuedCoin.setId((long) RandomUtils.nextIntInRange(1, 100_000_000));
        issuedCoin.setCoinType("FIXED");
        issuedCoin.setStartDate("2020-07-30T15:35:07.051+0300");
        issuedCoin.setEndDate("2020-12-14T00:00:00.000+0300");
        issuedCoin.setImage("5074bba7-4be2-4875-9d4a-56d9d9ccc7fa");
        issuedCoin.setImageGroupId(1546391L);
        issuedCoin.setNominal(100.0);
        issuedCoin.setBackgroundColor("#FF00AA");
        issuedCoin.setDescription("Heloo");
        if (authenticated) {
            issuedCoin.setUid(Long.parseLong(data.getIdValue()));
            issuedCoin.setTitle("Coin for " + data.getIdValue());
            issuedCoin.setSubtitle("Test stub coin");
            issuedCoin.setActivationToken("");
        } else {
            issuedCoin.setUid(null);
            issuedCoin.setTitle(null);
            issuedCoin.setSubtitle(null);
            issuedCoin.setActivationToken("token-shmoken-" + data.getIdValue());
        }
        return issuedCoin;
    }

    private List<StepOutputRow> readResults(IssueCoinsStep step) {
        return ytClient.read(
                actionTestHelper.getStepOutputPath(action.getId(), step.getId()),
                StepOutputRow.class
        );
    }

    private void assertCoinVarFilled(List<StepOutputRow> result) {
        assertCoinVarFilled(result, null);
    }

    private void assertCoinVarFilled(List<StepOutputRow> result, Integer expectedCoins) {
        for (StepOutputRow row : result) {
            StepOutputRow.Data data = row.getData();
            Assertions.assertNotNull(data);

            Map<String, YTreeNode> vars = data.getVars();
            Assertions.assertNotNull(vars);

            YTreeNode coins = vars.get("COINS");
            Assertions.assertNotNull(coins);

            Assertions.assertTrue(coins.isListNode());
            Optional.ofNullable(expectedCoins)
                    .ifPresent(expectedCoins1 -> Assertions.assertEquals(expectedCoins.intValue(),
                            coins.asList().size()));

            for (YTreeNode coinNode : coins.asList()) {
                Coin coin = COIN_SERIALIZER.deserialize(coinNode);
                Assertions.assertNotNull(coin);
                Assertions.assertEquals(EMAIL_COMPANY, coin.getReason());

                String activationToken = coin.getActivationToken();
                if (!Strings.isNullOrEmpty(activationToken)) {
                    Assertions.assertNotNull(coin.getActivationUrl(), "Activation url is not generated");
                    Assertions.assertNotNull(coin.getActivationDeeplink(), "Activation deeplink is not generated");
                }

                Assertions.assertEquals("https://avatars.mds.yandex.net" +
                                "/get-smart_shopping/1546391/5074bba7-4be2-4875-9d4a-56d9d9ccc7fa/328x328",
                        coin.getImage());
                Assertions.assertEquals(coin.getDefaultImage(), coin.getImage());

                String formattedDate = coin.getFormattedEndDate();
                Assertions.assertNotNull(formattedDate, "Formatted end date is not set");

                LocalDate endDate = LocalDate.parse(formattedDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                Assertions.assertEquals(coin.getEndDate().toLocalDate(), endDate);
            }
        }
    }

    private void waitForStepStatus(String stepId, Function<StepStatus<?>, Boolean> stepStatusMatcher) throws InterruptedException {
        actionTestHelper.waitFor(action.getId(), Duration.ofMinutes(25), receivedAction -> {
            ActionStatus actionStatus = receivedAction.getStatus();
            Assertions.assertNotNull(actionStatus);

            StepStatus<?> stepStatus = actionStatus.getSteps().get(stepId);
            if (stepStatus == null) {
                return false;
            }

            return stepStatusMatcher.apply(stepStatus);
        });
    }

    private boolean stepIsFinished(StepStatus<?> stepStatus) {
        StageStatus stageStatus = stepStatus.getStageStatus();

        Assertions.assertTrue(
                stageStatus == StageStatus.IN_PROGRESS || stageStatus == StageStatus.FINISHED,
                "Step status is " + stageStatus);

        return stageStatus == StageStatus.FINISHED;
    }

    private void requestLaunch() throws Exception {
        mockMvc.perform(
                post("/api/actions/{actionId}/steps/{stepId}/launch", action.getId(), step.getId())
        )
                .andExpect(status().isOk())
                .andDo(print());
    }

    private void prepareSegmentationResult(List<StepOutputRow> rows) {
        actionTestHelper.prepareSegmentationResult(action.getId(), rows);
    }

    private Action prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }
}
