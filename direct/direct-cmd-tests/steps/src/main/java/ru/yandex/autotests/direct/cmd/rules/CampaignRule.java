package ru.yandex.autotests.direct.cmd.rules;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignV2;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.steps.ApiAggregationSteps;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.NullAwareBeanUtilsBean;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.User;

import static java.util.stream.Collectors.toList;
import static jersey.repackaged.com.google.common.base.Preconditions.checkState;
import static ru.yandex.autotests.direct.cmd.rules.CampaignRuleHelper.clearMobileApp;
import static ru.yandex.autotests.direct.cmd.rules.CampaignRuleHelper.createDefaultMobileApp;
import static ru.yandex.autotests.direct.utils.BaseSteps.getInstance;

public class CampaignRule implements TestRule, NeedsCmdSteps {

    private static final LocalDate START = LocalDate.now().plusDays(1);

    protected final Logger log;
    protected CampaignTypeEnum mediaType;
    protected CampaignStrategy campStrategy;
    protected Map<String, Object> graphQlCampStrategy;
    protected List<String> minusWords;
    protected String saveCampTemplate;
    protected String ulogin;
    protected Long campaignId;
    protected SaveCampRequest saveCampRequest;
    private boolean testFailed;
    private boolean testBroken;
    private DirectCmdSteps directCmdSteps;

    //rmp fields
    private CampaignRuleHelper.MobileAppData appData;

    public CampaignRule() {
        this.log = LoggerFactory.getLogger(this.getClass());
    }

    public void execute() {
        start();
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    start();
                    base.evaluate();
                } catch (AssertionError e) {
                    testFailed = true;
                    throw e;
                } catch (Throwable t) {
                    testBroken = true;
                    throw t;
                } finally {
                    finish();
                }
            }
        };
    }


    @Override
    public DirectCmdSteps getDirectCmdSteps() {
        return directCmdSteps;
    }

    @Override
    public <T extends NeedsCmdSteps> T withDirectCmdSteps(DirectCmdSteps directCmdSteps) {
        this.directCmdSteps = directCmdSteps;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends CampaignRule> T withUlogin(String ulogin) {
        this.ulogin = ulogin;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends CampaignRule> T withMediaType(CampaignTypeEnum mediaType) {
        this.mediaType = mediaType;

        if (mediaType == CampaignTypeEnum.INTERNAL_AUTOBUDGET) {
            return (T) this;
        }

        return this.withCampTemplate(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType));
    }

    public <T extends CampaignRule> T withCampTemplate(String templateName) {
        this.saveCampTemplate = templateName;
        return (T) this;
    }

    public <T extends CampaignRule> T withMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
        return (T) this;
    }

    public <T extends CampaignRule> T overrideCampTemplate(SaveCampRequest saveCampRequest) {
        try {
            new NullAwareBeanUtilsBean().copyProperties(getSaveCampRequest(), saveCampRequest);
            return (T) this;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("ошибка при записи полей бина кампании", e);
        }
    }

    public <T extends CampaignRule> T withCampStrategy(CampaignStrategy strategy) {
        this.campStrategy = strategy;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends CampaignRule> T withGraphQlCampStrategy(Map<String, Object> strategy) {
        this.graphQlCampStrategy = strategy;
        return (T) this;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    protected void start() {
        createCampaign();
    }

    protected void finish() {
        if (campaignId == null) {
            return;
        }
        if (testFailed && !DirectTestRunProperties.getInstance().isDirectCmdDeleteCampOnFailed()) {
            return;
        }
        if (testBroken && !DirectTestRunProperties.getInstance().isDirectCmdDeleteCampOnBroken()) {
            return;
        }

        if (ulogin != null) {
            directCmdSteps.authSteps().authenticate(User.get(Logins.SUPER_LOGIN));
        }

        deleteCampaign();

        if (isMobileCamp()) {
            String authLogin = getDirectCmdSteps().context().getAuthConfig().getLogin();
            try {
                clearMobileApp(getAppData(), ulogin == null ? authLogin : ulogin);
            } catch (Exception e) {
                log.info("Error while deleting mobileContent", e);
            }
        }
    }

    private void deleteCampaign() {
        ApiStepsRule rule = new ApiStepsRule(this.getDirectCmdSteps().context().getProperties());
        getInstance(ApiAggregationSteps.class, rule.userSteps()).makeCampaignReadyForDelete(campaignId);
        if (ulogin == null) {
            log.error("campaign {} may not be deleted: specify Ulogin in rule", campaignId);
        }
        directCmdSteps.campaignSteps().deleteCampaign(ulogin, campaignId);
    }

    private void createCampaign() {
        if (mediaType == CampaignTypeEnum.INTERNAL_AUTOBUDGET) {
            Map<String, Object> request = buildAddInternalAutobudgetCampaignRequest();

            campaignId = getDirectCmdSteps().campaignGraphQlSteps().addCampaign(ulogin, request);
            return;
        }

        getSaveCampRequest().setMediaType(mediaType.getValue());
        getSaveCampRequest().setUlogin(ulogin);

        if (campStrategy != null) {
            getSaveCampRequest().setJsonStrategy(campStrategy);
        }
        if (minusWords != null) {
            getSaveCampRequest().setJsonCampaignMinusWords(minusWords);
        }

        if (isMobileCamp()) {
            String authLogin = getDirectCmdSteps().context().getAuthConfig().getLogin();
            appData = createDefaultMobileApp(ulogin == null ? authLogin : ulogin);
            getSaveCampRequest().setMobileAppId(getAppData().getMobileAppId());
        }

        campaignId = getDirectCmdSteps().campaignSteps().saveNewCampaign(getSaveCampRequest());
    }

    private Map<String, Object> buildAddInternalAutobudgetCampaignRequest() {
        List<List<Integer>> timeBoard = IntStream.range(0, 7).boxed()
                .map(i -> IntStream.range(0, 24).boxed()
                        .map(j -> 100)
                        .collect(toList()))
                .collect(toList());

        Map<String, Object> strategy = graphQlCampStrategy != null ? graphQlCampStrategy
                : defaultGraphQlCampStrategy();

        return ImmutableMap.<String, Object>builder()
                .put("internalAutobudgetCampaign", ImmutableMap.<String, Object>builder()
                        .put("name", "CmdSteps: Автобюджетная внутренняя реклама")
                        .put("startDate", START.toString())
                        .put("notification", ImmutableMap.<String, Object>builder()
                                .put("smsSettings", ImmutableMap.<String, Object>builder()
                                        .put("smsTime", ImmutableMap.<String, Object>builder()
                                                .put("startTime", ImmutableMap.<String, Object>builder()
                                                        .put("hour", 9)
                                                        .put("minute", 0)
                                                        .build())
                                                .put("endTime", ImmutableMap.<String, Object>builder()
                                                        .put("hour", 21)
                                                        .put("minute", 0)
                                                        .build())
                                                .build())
                                        .put("enableEvents", Collections.emptyList())
                                        .build())
                                .put("emailSettings", ImmutableMap.<String, Object>builder()
                                        .put("stopByReachDailyBudget", true)
                                        .put("checkPositionInterval", "M_60")
                                        .put("xlsReady", true)
                                        .put("email", ulogin + "@yandex.ru")
                                        .build())
                                .build())
                        .put("biddingStategy", strategy)
                        .put("metrikaCounters", Collections.emptyList())
                        .put("attributionModel", "LAST_YANDEX_DIRECT_CLICK")
                        .put("timeTarget", ImmutableMap.<String, Object>builder()
                                .put("enabledHolidaysMode", false)
                                .put("holidaysSettings", ImmutableMap.<String, Object>builder()
                                        .put("isShow", false)
                                        .build())
                                .put("idTimeZone", "130")
                                .put("timeBoard", timeBoard)
                                .put("useWorkingWeekends", true)
                                .build())
                        .put("isMobile", false)
                        .put("placeId", 1135)
                        .put("pageId", Collections.emptyList())
                        .build())
                .build();
    }

    private Map<String, Object> defaultGraphQlCampStrategy() {
        return ImmutableMap.<String, Object>builder()
                .put("strategyName", "AUTOBUDGET")
                .put("platform", "BOTH")
                .put("strategyData", ImmutableMap.<String, Object>builder()
                        .put("sum", 48934)
                        .build())
                .build();
    }

    protected boolean isMobileCamp() {
        return mediaType == CampaignTypeEnum.MOBILE;
    }

    protected CampaignRuleHelper.MobileAppData getAppData() {
        checkState(isMobileCamp(), "can only be used for mobile campaign");
        return appData;
    }

    public SaveCampRequest getSaveCampRequest() {
        if (saveCampRequest == null) {
            saveCampRequest = BeanLoadHelper.loadCmdBean(saveCampTemplate, SaveCampRequest.class);
        }
        return saveCampRequest;
    }

    public CampaignTypeEnum getMediaType() {
        return mediaType;
    }

    public CampaignV2 getCurrentCampaign() {
        return directCmdSteps.campaignSteps().getEditCamp(campaignId, ulogin).getCampaign();
    }
}
