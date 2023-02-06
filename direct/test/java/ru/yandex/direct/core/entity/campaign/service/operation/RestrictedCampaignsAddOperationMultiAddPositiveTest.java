package ru.yandex.direct.core.entity.campaign.service.operation;

import java.time.LocalDate;
import java.util.List;

import jdk.jfr.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORD_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusWord;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultSmartCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.DateDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsAddOperationMultiAddPositiveTest {

    private static final int COUNTER_ID = 1;
    private static final Long VALID_GOAL_ID = 55L;
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    private CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    public RbacService rbacService;

    @Autowired
    private Steps steps;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    private GoalUtilsService goalUtilsService;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private ClientInfo defaultClient;
    private Long mobileAppId;


    @Before
    public void before() {
        defaultClient = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), FeatureName.DISABLE_BILLING_AGGREGATES, true);
        metrikaClientStub.addUserCounter(defaultClient.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, VALID_GOAL_ID.intValue());

        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(defaultClient, STORE_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();
    }

    @Test
    @Description("Проверяем добавление кампаний разных типов в рамках одной команды. " +
            "Если тест упал, то вероятно нужны правки в BaseCampaignRepositoryTypeSupport")
    public void addAllCampaignTypesInOneCommand() {
        var textCampaign = defaultTextCampaign();
        var smartCampaign = defaultSmartCampaign()
                .withMetrikaCounters(List.of((long) COUNTER_ID));
        var dynamicCampaign = defaultDynamicCampaign();
        var mobileContentCampaign = defaultMobileContentCampaign()
                .withMobileAppId(mobileAppId);
        var defaultCpmBannerCampaign = defaultCpmBannerCampaign();
        var contentPromotionCampaign = defaultContentPromotionCampaign();

        var result = addCampaigns(List.of(textCampaign, smartCampaign, dynamicCampaign,
                mobileContentCampaign, defaultCpmBannerCampaign, contentPromotionCampaign));
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        assertThat(result.getResult().size()).isEqualTo(6);
        assertThat(result.get(0).getResult()).isNotNull();
        assertThat(result.get(1).getResult()).isNotNull();
        assertThat(result.get(2).getResult()).isNotNull();
        assertThat(result.get(3).getResult()).isNotNull();
        assertThat(result.get(4).getResult()).isNotNull();
        assertThat(result.get(5).getResult()).isNotNull();
    }

    @Test
    @Description("Добавляем 4 кампании - первая ок, у второй ошибка на этапе preValidate, у третьей на этапе " +
            "validate и четвёртая ок")
    public void preValidateAndValidateTogether() {
        var invalidMinusWord = "longLongLongLongLongLongLongLongLongLong";
        LocalDate invalidStartDate = LocalDate.of(2000, 1, 1);

        var campaign1 = defaultTextCampaign();

        var campaign2 = defaultTextCampaign()
                .withMinusKeywords(List.of(invalidMinusWord))
                .withStartDate(invalidStartDate);

        // Проверим норм ли вешаются дефекты на обычные ошибки из validate, не смещаются ли индекс из за ошибок
        // с этапа preValidate
        var campaign3 = defaultTextCampaign()
                .withStartDate(invalidStartDate);

        // Проверим не смещаются ли индексы из за ошибок с этапа preValidate
        var campaign4 = defaultTextCampaign();

        var result = addCampaigns(List.of(campaign1, campaign2, campaign3, campaign4));

        assertThat(result.getValidationResult().getSubResults().get(index(0)).flattenErrors()).isEmpty();
        assertThat(result.getValidationResult().getSubResults().get(index(0)).flattenWarnings()).isEmpty();

        // В кампании невалидные минус-фразы и startDate. Проверим, что есть дефект на минус фразе, а других дефектов
        // нет (в том числе на startDate) - цель, проверить что когда preValidate вешает дефекты - мы нормально
        // отрабатываем (не падаем, не вешаем несуществующие дефекты на системные поля).
        // Проверяя то, что на startDate нет дефекта, мы по косвенным признакам проверяем, что валидация минус-фраз
        // действительно находится в preValidate
        Assert.assertThat(result.getValidationResult().getSubResults().get(index(1)).flattenErrors(),
                contains(validationError(path(field(CampaignWithMinusKeywords.MINUS_KEYWORDS), index(0)),
                        maxLengthMinusWord(WORD_MAX_LENGTH, List.of(invalidMinusWord)))
                ));
        assertThat(result.getValidationResult().getSubResults().get(index(1)).flattenWarnings()).isEmpty();

        Assert.assertThat(result.getValidationResult().getSubResults().get(index(2)).flattenErrors(),
                anyOf(
                        contains(validationError(path(field(CampaignWithMinusKeywords.START_DATE)),
                                greaterThanOrEqualTo(TODAY))),
                        // на случай если во время прогона теста часы перешагнули через 0 часов
                        contains(validationError(path(field(CampaignWithMinusKeywords.START_DATE)),
                                greaterThanOrEqualTo(TOMORROW)))
                ));
        assertThat(result.getValidationResult().getSubResults().get(index(2)).flattenWarnings()).isEmpty();

        assertThat(result.getValidationResult().getSubResults().get(index(3)).flattenErrors()).isEmpty();
        assertThat(result.getValidationResult().getSubResults().get(index(3)).flattenWarnings()).isEmpty();
    }

    private MassResult<Long> addCampaigns(List<? extends BaseCampaign> campaignsToAdd) {
        var options = new CampaignOptions();

        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                campaignsToAdd,
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade,
                dslContextProvider,
                rbacService, options, metrikaClientFactory, goalUtilsService);
        return addOperation.prepareAndApply();
    }

}
