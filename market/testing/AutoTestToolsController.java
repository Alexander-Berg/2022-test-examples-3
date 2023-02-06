package ru.yandex.market.mbi.api.testing;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import ru.yandex.market.core.autopayment.AutoPaymentSettingsService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.cutoff.model.DuplicateCutoffException;
import ru.yandex.market.core.moderation.Phase;
import ru.yandex.market.core.moderation.ReleaseHaltedModerationEntryPoint;
import ru.yandex.market.core.moderation.TestingShop;
import ru.yandex.market.core.moderation.feed.passed.ModerationFeedsLoadCheckPassedEntryPoint;
import ru.yandex.market.core.moderation.passed.PassModerationEntryPoint;
import ru.yandex.market.core.moderation.request.ModerationRequestEntryPoint;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ValueType;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.validation.ConstraintViolationsException;
import ru.yandex.market.mbi.api.client.entity.ApiResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.controller.ApiConstraintViolationsErrorResponse;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * @author zoom
 */
@Profile({"testing", "development"})
@Controller
public class AutoTestToolsController {

    private static final Logger log = LoggerFactory.getLogger(AutoTestToolsController.class);

    private final ModerationFeedsLoadCheckPassedEntryPoint moderationFeedsLoadCheckPassedEntryPoint;
    private final PassModerationEntryPoint passModerationEntryPoint;
    private final ReleaseHaltedModerationEntryPoint releaseHaltedModerationEntryPoint;
    private final ModerationRequestEntryPoint moderationRequestEntryPoint;
    private final ProtocolService protocolService;
    private final ParamService paramService;
    private final SandboxRepository sandboxRepository;
    private final CampaignService campaignService;
    private final AutoPaymentSettingsService autoPaymentSettingsService;
    private final CutoffService cutoffService;

    @SuppressWarnings("all")
    @Autowired
    public AutoTestToolsController(ModerationFeedsLoadCheckPassedEntryPoint moderationFeedsLoadCheckPassedEntryPoint,
                                   PassModerationEntryPoint passModerationEntryPoint,
                                   ReleaseHaltedModerationEntryPoint releaseHaltedModerationEntryPoint,
                                   ModerationRequestEntryPoint moderationRequestEntryPoint,
                                   ProtocolService protocolService,
                                   ParamService paramService,
                                   SandboxRepository sandboxRepository,
                                   CampaignService campaignService,
                                   AutoPaymentSettingsService autoPaymentSettingsService,
                                   CutoffService cutoffService) {
        this.moderationFeedsLoadCheckPassedEntryPoint = moderationFeedsLoadCheckPassedEntryPoint;
        this.passModerationEntryPoint = passModerationEntryPoint;
        this.releaseHaltedModerationEntryPoint = releaseHaltedModerationEntryPoint;
        this.moderationRequestEntryPoint = moderationRequestEntryPoint;
        this.protocolService = protocolService;
        this.paramService = paramService;
        this.sandboxRepository = sandboxRepository;
        this.campaignService = campaignService;
        this.autoPaymentSettingsService = autoPaymentSettingsService;
        this.cutoffService = cutoffService;
    }

    /**
     * Запустить CPA-премодерацию.
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/premoderation/cpa", method = RequestMethod.POST)
    public Object requestCpaCheck(@PathVariable("shopId") long shopId, HttpServletResponse resp) {
        SystemActionContext systemActionContext = new SystemActionContext(ActionType.TEST_ACTION);
        return protocolService.actionInTransaction(systemActionContext, (transactionState, actionId) -> {
            ShopActionContext shopContext = new ShopActionContext(actionId, shopId);
            moderationRequestEntryPoint.requestCPAModeration(shopContext);
            return ApiResponse.OK;
        });
    }

    /**
     * Безусловно засчитать фид магазина успешно загруженным индексатором.
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/premoderation/{program}/feed/check", method = RequestMethod.DELETE)
    public ApiResponse passFeedLoadCheck(@PathVariable("shopId") long shopId, @PathVariable ShopProgram program) {
        moderationFeedsLoadCheckPassedEntryPoint.passForced(
                new SystemActionContext(ActionType.PREREVOKE_CHECK, Phase.CHECK_FEEDS_LOAD.getComment()),
                new TestingShop(sandboxRepository.loadOrFail(shopId, program).getTestingId(), shopId));
        return ApiResponse.OK;
    }

    /**
     * Успешно завершить cpc-премодерацию.
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/premoderation/cpc", method = RequestMethod.DELETE)
    public ApiResponse passCheck(@PathVariable("shopId") long shopId) {
        passModerationEntryPoint.pass(
                new SystemActionContext(ActionType.PREREVOKE_CHECK, Phase.CHECK_TESTING_RESULTS.getComment()),
                new TestingShop(sandboxRepository.loadOrFail(shopId, ShopProgram.CPC).getTestingId(), shopId));
        return ApiResponse.OK;
    }

    /**
     * Разблокировать возможность магазина пройти премодерацию (если, к примеру, он превысил кол-во попыток её пройти).
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/premoderation/lock", method = RequestMethod.DELETE)
    public ApiResponse restartModeration(@PathVariable("shopId") long shopId) {
        releaseHaltedModerationEntryPoint.release(
                new SystemActionContext(ActionType.REGISTER_MODERATION_CHECK_RESULT), shopId);
        return ApiResponse.OK;
    }

    /**
     * Запускает cpc премодерацию (точный тип премодерации определяется автоматически).
     * Эмулирует нажатие кнопки "Отправить на проверку".
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/premoderation/cpc", method = RequestMethod.POST)
    public Object requestModeration(@PathVariable("shopId") long shopId, HttpServletResponse resp) {
        SystemActionContext systemActionContext =
                new SystemActionContext(ActionType.USER_PUSH_BUTTON);
        return protocolService.actionInTransaction(systemActionContext, (transactionStatus, actionId) -> {
            ShopActionContext shopActionContext = new ShopActionContext(actionId, shopId);
            moderationRequestEntryPoint.requestCPCModeration(shopActionContext);
            return ApiResponse.OK;
        });
    }

    /**
     * Начинает тестирование. Использовать, чтобы не ждать 90 дней.check
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/premoderation/{program}/start", method = RequestMethod.POST)
    public Object startTesting(@PathVariable("shopId") long shopId,
                               @PathVariable ShopProgram program) {
        SystemActionContext systemActionContext =
                new SystemActionContext(ActionType.PREREVOKE_CHECK, Phase.START_TESTING_APPROVED_SHOPS.getComment());
        return protocolService.actionInTransaction(systemActionContext, (transactionStatus, actionId) -> {
            ShopActionContext shopActionContext = new ShopActionContext(actionId, shopId);
            moderationRequestEntryPoint.startTesting(shopActionContext, program);
            return ApiResponse.OK;
        });
    }

    /**
     * Устанавливает параметры магазина.
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/params/{paramTypeId}", method = RequestMethod.POST)
    public Object updateSystemParam(@PathVariable("shopId") int shopId,
                                    @PathVariable("paramTypeId") int paramTypeId,
                                    @RequestParam("value") String value) {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.CHANGE_PARAM),
                (transactionStatus, actionId) -> {
                    ParamType paramType = ParamType.getParamType(paramTypeId);
                    if (paramType.getValueType() == ValueType.BOOLEAN) {
                        paramService.setParam(
                                new BooleanParamValue(paramType, shopId, Boolean.parseBoolean(value)), actionId);
                    } else {
                        throw new UnsupportedOperationException(
                                "ParamType: " + paramType.getValueType() + " is not supported");
                    }
                }
        );
        return ApiResponse.OK;
    }

    /**
     * Открываем cutoff.
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/cutoffs/{cutoffTypeId}", method = RequestMethod.PUT)
    public ApiResponse openCutoff(@PathVariable("shopId") long shopId,
                                  @PathVariable("cutoffTypeId") int cutoffTypeId) {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.MANAGE_CUTOFF_CLAIM),
                (transactionStatus, actionId) -> {
                    try {
                        cutoffService.openCutoff(shopId, CutoffType.byId(cutoffTypeId), actionId);
                    } catch (DuplicateCutoffException ex) {
                        log.error("Failed to open cutoff", ex);
                    }
                }
        );
        return ApiResponse.OK;
    }

    /**
     * Закрываем cutoff.
     */
    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/cutoffs/{cutoffTypeId}", method = RequestMethod.DELETE)
    public ApiResponse closeCutoff(@PathVariable("shopId") long shopId,
                                   @PathVariable("cutoffTypeId") int cutoffTypeId) {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.MANAGE_CUTOFF_CLAIM),
                (transactionStatus, actionId) -> cutoffService.closeCutoff(
                        shopId, CutoffType.byId(cutoffTypeId), actionId
                )
        );
        return ApiResponse.OK;
    }

    @ResponseBody
    @GetMapping("/testing/shops/sync/campaign")
    public ApiResponse syncCampaign(@RequestParam("campaign_id") long campaignId) {
        CampaignInfo campaign = campaignService.getMarketCampaign(campaignId);
        campaignService.syncCampaignWithBalance(campaign,
                SystemActionContext.MARKET_SYSTEM_ID,
                campaignService.getProductId(campaignId));
        return ApiResponse.OK;
    }

    @ResponseBody
    @DeleteMapping("auto-payment/settings")
    public ApiResponse deleteAutoPaymentSettings(@RequestParam("campaign_id") long campaignId) {
        autoPaymentSettingsService.deleteAutoPaymentSettings(campaignId);
        return ApiResponse.OK;
    }

    @ResponseBody
    @ResponseStatus(CONFLICT)
    @ExceptionHandler(ConstraintViolationsException.class)
    public ApiConstraintViolationsErrorResponse handleConflict(ConstraintViolationsException e) {
        return new ApiConstraintViolationsErrorResponse(e);
    }

    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public GenericCallResponse handleCommonException(Exception e) {
        log.error("Internal PremoderationTestingController error", e);
        return GenericCallResponse.exception(e);
    }
}
