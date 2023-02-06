package ru.yandex.market.pricelabs.integration.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.MockMvcProxyHttpException;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApiInterfaces;
import ru.yandex.market.pricelabs.api.search.AutostrategiesSearch;
import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategiesOfferCount;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategiesOfferFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterVendor;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad.StateEnum;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyOffer;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPO;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsDRR;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsPOS;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsVPOS;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.BlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.StrategyType;
import ru.yandex.market.pricelabs.processing.CoreTables;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.services.database.SequenceService;
import ru.yandex.market.pricelabs.services.database.SequenceServiceImpl;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.yt.YtConfiguration;
import ru.yandex.market.yt.binding.BindingTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategiesOfferFilter.AutostrategyTypeEnum;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter.TypeEnum.SIMPLE;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter.TypeEnum.VENDOR;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPA;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPO;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.DRR;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.POS;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.VPOS;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.assertThrowsWithMessage;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyState;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.autostrategy;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.reorder;

@Slf4j
public abstract class AbstractAutostrategiesApiTest extends AbstractAutostrategiesTestsOffersInitialisation {

    public static final int SHOP1 = 1;
    public static final int SHOP2 = 2;
    static final int SHOP3 = 3;

    static final int CATEGORY_DEFAULT = 90401;

    static final int UNKNOWN_SHOP = 332211;
    private final String allTargets = Stream.of(AutostrategyTarget.values())
            .map(Enum::name)
            .collect(Collectors.joining("|"));
    protected PublicAutostrategiesApiInterfaces publicApi;
    @Autowired
    private PublicAutostrategiesApi publicApiBean;
    @Autowired
    @Qualifier("whiteSearch")
    private AutostrategiesSearch whiteSearch;
    @Autowired
    @Qualifier("blueSearch")
    private AutostrategiesSearch blueSearch;
    @Autowired
    @Qualifier("vendorBlueSearch")
    private AutostrategiesSearch vendorBlueSearch;
    @Autowired
    @Qualifier("autostrategiesMetaWhite")
    private AutostrategiesMetaProcessor metaWhite;
    @Autowired
    @Qualifier("autostrategiesMetaBlue")
    private AutostrategiesMetaProcessor metaBlue;
    @Autowired
    @Qualifier("autostrategiesMetaVendorBlue")
    private AutostrategiesMetaProcessor metaVendorBlue;
    @Autowired
    private SequenceService sequenceService;
    @Autowired
    private YtConfiguration ytCfg;
    @Autowired
    private CoreTables coreTables;
    private YtScenarioExecutor<Offer> offersExecutor;
    private YtScenarioExecutor<Autostrategy> autostrategiesExecutor;
    private YtScenarioExecutor<AutostrategyState> statesExecutor;
    private AutostrategyTarget autoTarget;
    private String target;
    private AutostrategiesMetaProcessor metaProcessor;

    private static AutostrategySave autostrategyInit(TypeEnum type, Consumer<AutostrategySave> init) {
        var auto = autostrategy("s1", type);
        init.accept(auto);
        return auto;
    }

    private static AutostrategySave restoreSave(AutostrategySave save) {
        var filter = save.getFilter();
        var filterType = Utils.nvl(filter.getType(), SIMPLE);
        if (filterType != SIMPLE) {
            filter.setSimple(new AutostrategyFilterSimple()
                    .vendors(List.of())
                    .categories(List.of())
                    .excludeCategories(List.of())
                    .offerIds(List.of())
            );
        }
        if (filterType != VENDOR) {
            filter.setVendor(new AutostrategyFilterVendor()
                    .shops(List.of())
                    .models(List.of())
                    .businesses(List.of())
            );
        }

        var settings = save.getSettings();
        var settingsType = settings.getType();
        if (settingsType != CPO) {
            settings.setCpo(new AutostrategySettingsCPO());
        }
        if (settingsType != DRR) {
            settings.setDrr(new AutostrategySettingsDRR());
        }
        if (settingsType != POS) {
            settings.setPos(new AutostrategySettingsPOS());
        }
        if (settingsType != VPOS) {
            settings.setVpos(new AutostrategySettingsVPOS());
        }
        if (settingsType != CPA) {
            settings.setCpa(new AutostrategySettingsCPA());
        }
        return save;
    }

    private static AutostrategySave transformLoad(AutostrategyLoad as) {
        return Utils.fromJsonString(Utils.toJsonString(as), AutostrategySave.class);
    }

    private static List<Integer> toAutostrategyId(List<AutostrategyLoad> list) {
        return list.stream()
                .map(AutostrategyLoad::getId)
                .collect(Collectors.toList());
    }

    private static String valueMustBeString(String value, int number, boolean isBigger) {
        return String.format("%s value must be %s %d", value, isBigger ? ">" : "<=", number);
    }

    //CHECKSTYLE:OFF
    private static Object[][] invalidAutostrategies() {
        return new Object[][]{
                {autostrategyInit(CPO, auto -> auto.setName("")), "name is required"},
                {autostrategyInit(CPO, auto -> auto.setName(null)), "Validation failed for argument [1] in public org" +
                        ".springframework.http.ResponseEntity<ru.yandex.market.pricelabs.generated.server.pub.model" +
                        ".AutostrategyLoad> ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi" +
                        ".autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs.generated.server.pub.model" +
                        ".AutostrategySave,java.lang.String): [Field error in object 'autostrategySave' on field " +
                        "'name': rejected value [null]; codes [NotNull.autostrategySave.name,NotNull.name,NotNull" +
                        ".java.lang.String,NotNull]; arguments [org.springframework.context.support" +
                        ".DefaultMessageSourceResolvable: codes [autostrategySave.name,name]; arguments []; default " +
                        "message [name]]; default message [must not be null]]"},
                {autostrategyInit(CPO, auto -> auto.setEnabled(null)), "Validation failed for argument [1] in public " +
                        "org.springframework.http.ResponseEntity<ru.yandex.market.pricelabs.generated.server.pub" +
                        ".model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi" +
                        ".autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs.generated.server.pub.model" +
                        ".AutostrategySave,java.lang.String): [Field error in object 'autostrategySave' on field " +
                        "'enabled': rejected value [null]; codes [NotNull.autostrategySave.enabled,NotNull.enabled," +
                        "NotNull.java.lang.Boolean,NotNull]; arguments [org.springframework.context.support" +
                        ".DefaultMessageSourceResolvable: codes [autostrategySave.enabled,enabled]; arguments []; " +
                        "default message [enabled]]; default message [must not be null]]"},
                {autostrategyInit(CPO, auto -> auto.setFilter(null)), "Validation failed for argument [1] in public " +
                        "org.springframework.http.ResponseEntity<ru.yandex.market.pricelabs.generated.server.pub" +
                        ".model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi" +
                        ".autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs.generated.server.pub.model" +
                        ".AutostrategySave,java.lang.String): [Field error in object 'autostrategySave' on field " +
                        "'filter': rejected value [null]; codes [NotNull.autostrategySave.filter,NotNull.filter," +
                        "NotNull.ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter,NotNull]; " +
                        "arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes " +
                        "[autostrategySave.filter,filter]; arguments []; default message [filter]]; default message " +
                        "[must not be null]]"},
                {autostrategyInit(CPO, auto -> auto.setSettings(null)), "settings is required"},
                {autostrategyInit(CPO, auto -> auto.getFilter().setType(null)), "Validation failed for argument [1] " +
                        "in public org.springframework.http.ResponseEntity<ru.yandex.market.pricelabs.generated" +
                        ".server.pub.model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api" +
                        ".PublicAutostrategiesApi.autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategySave,java.lang.String): [Field error in object " +
                        "'autostrategySave' on field 'filter.type': rejected value [null]; codes [NotNull" +
                        ".autostrategySave.filter.type,NotNull.filter.type,NotNull.type,NotNull.ru.yandex.market" +
                        ".pricelabs.generated.server.pub.model.AutostrategyFilter$TypeEnum,NotNull]; arguments [org" +
                        ".springframework.context.support.DefaultMessageSourceResolvable: codes [autostrategySave" +
                        ".filter.type,filter.type]; arguments []; default message [filter.type]]; default message " +
                        "[must not be null]]"},
                {autostrategyInit(CPO, auto -> auto.getFilter().getSimple().setPriceFrom(-1L)), valueMustBeString(
                        "priceFrom", 1, true)},
                {autostrategyInit(CPO, auto -> auto.getFilter().getSimple().setPriceTo(-1L)), valueMustBeString(
                        "priceTo", 1, true)},
                {autostrategyInit(CPO, auto -> auto.getSettings().setType(null)), "Validation failed for argument [1]" +
                        " in public org.springframework.http.ResponseEntity<ru.yandex.market.pricelabs.generated" +
                        ".server.pub.model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api" +
                        ".PublicAutostrategiesApi.autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategySave,java.lang.String): [Field error in object " +
                        "'autostrategySave' on field 'settings.type': rejected value [null]; codes [NotNull" +
                        ".autostrategySave.settings.type,NotNull.settings.type,NotNull.type,NotNull.ru.yandex.market" +
                        ".pricelabs.generated.server.pub.model.AutostrategySettings$TypeEnum,NotNull]; arguments [org" +
                        ".springframework.context.support.DefaultMessageSourceResolvable: codes [autostrategySave" +
                        ".settings.type,settings.type]; arguments []; default message [settings.type]]; default " +
                        "message [must not be null]]"},
                {autostrategyInit(CPO, auto -> auto.getSettings().setCpo(null)), "cpo configuration block is required"},
                {autostrategyInit(CPO, auto -> auto.getSettings().getCpo().setCpo(null)), "Validation failed for " +
                        "argument [1] in public org.springframework.http.ResponseEntity<ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api" +
                        ".PublicAutostrategiesApi.autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategySave,java.lang.String): [Field error in object " +
                        "'autostrategySave' on field 'settings.cpo.cpo': rejected value [null]; codes [NotNull" +
                        ".autostrategySave.settings.cpo.cpo,NotNull.settings.cpo.cpo,NotNull.cpo,NotNull.java.lang" +
                        ".Long,NotNull]; arguments [org.springframework.context.support" +
                        ".DefaultMessageSourceResolvable: codes [autostrategySave.settings.cpo.cpo,settings.cpo.cpo];" +
                        " arguments []; default message [settings.cpo.cpo]]; default message [must not be null]]"},
                {autostrategyInit(CPO, auto -> auto.getSettings().getCpo().setCpo(0L)),
                        valueMustBeString("cpo", 1, true)},
                {autostrategyInit(CPO, auto -> auto.getSettings().getCpo().setCpo(-1L)),
                        valueMustBeString("cpo", 1, true)},
                {autostrategyInit(CPO, auto -> auto.getSettings().getCpo().setCpo(65536L)),
                        valueMustBeString("cpo", 65535, false)},
                {autostrategyInit(DRR, auto -> auto.getSettings().setDrr(null)), "drr configuration block is required"},
                {autostrategyInit(DRR, auto -> auto.getSettings().getDrr().setTakeRate(null)), "Validation failed for" +
                        " argument [1] in public org.springframework.http.ResponseEntity<ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api" +
                        ".PublicAutostrategiesApi.autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategySave,java.lang.String): [Field error in object " +
                        "'autostrategySave' on field 'settings.drr.takeRate': rejected value [null]; codes [NotNull" +
                        ".autostrategySave.settings.drr.takeRate,NotNull.settings.drr.takeRate,NotNull.takeRate," +
                        "NotNull.java.lang.Long,NotNull]; arguments [org.springframework.context.support" +
                        ".DefaultMessageSourceResolvable: codes [autostrategySave.settings.drr.takeRate,settings.drr" +
                        ".takeRate]; arguments []; default message [settings.drr.takeRate]]; default message [must " +
                        "not be null]]"},
                {autostrategyInit(DRR, auto -> auto.getSettings().getDrr().setTakeRate(0L)), valueMustBeString(
                        "takeRate", 1, true)},
                {autostrategyInit(DRR, auto -> auto.getSettings().getDrr().setTakeRate(-1L)), valueMustBeString(
                        "takeRate", 1, true)},
                {autostrategyInit(DRR, auto -> auto.getSettings().getDrr().setTakeRate(10001L)), valueMustBeString(
                        "takeRate", 10000, false)},
                {autostrategyInit(POS, auto -> auto.getSettings().setPos(null)), "pos configuration block is required"},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(null)), "Validation " +
                        "failed for argument [1] in public org.springframework.http.ResponseEntity<ru.yandex.market" +
                        ".pricelabs.generated.server.pub.model.AutostrategyLoad> ru.yandex.market.pricelabs.api.api" +
                        ".PublicAutostrategiesApi.autostrategyPost(java.lang.Integer,ru.yandex.market.pricelabs" +
                        ".generated.server.pub.model.AutostrategySave,java.lang.String): [Field error in object " +
                        "'autostrategySave' on field 'settings.pos.maxBid': rejected value [null]; codes [NotNull" +
                        ".autostrategySave.settings.pos.maxBid,NotNull.settings.pos.maxBid,NotNull.maxBid,NotNull" +
                        ".java.lang.Long,NotNull]; arguments [org.springframework.context.support" +
                        ".DefaultMessageSourceResolvable: codes [autostrategySave.settings.pos.maxBid,settings.pos" +
                        ".maxBid]; arguments []; default message [settings.pos.maxBid]]; default message [must not be" +
                        " null]]"},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(0L)),
                        valueMustBeString("maxBid", 30, true)},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(-1L)),
                        valueMustBeString("maxBid", 30, true)},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(252001L)),
                        valueMustBeString("maxBid", 252000, false)},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(29L)),
                        valueMustBeString("maxBid", 30, true)},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().maxBid(45L).setPosition(null)),
                        "Validation failed for argument [1] in public org.springframework.http.ResponseEntity<ru" +
                                ".yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad> ru.yandex" +
                                ".market.pricelabs.api.api.PublicAutostrategiesApi.autostrategyPost(java.lang" +
                                ".Integer,ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave,java" +
                                ".lang.String): [Field error in object 'autostrategySave' on field 'settings.pos" +
                                ".position': rejected value [null]; codes [NotNull.autostrategySave.settings.pos" +
                                ".position,NotNull.settings.pos.position,NotNull.position,NotNull.java.lang.Integer," +
                                "NotNull]; arguments [org.springframework.context.support" +
                                ".DefaultMessageSourceResolvable: codes [autostrategySave.settings.pos.position," +
                                "settings.pos.position]; arguments []; default message [settings.pos.position]]; " +
                                "default message [must not be null]]"},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().maxBid(45L).setPosition(0)),
                        valueMustBeString("position", 1, true)},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().maxBid(45L).setPosition(-1)),
                        valueMustBeString("position", 1, true)},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().maxBid(45L).setPosition(11)),
                        valueMustBeString("position", 10, false)},
                {autostrategyInit(VPOS, auto -> auto.getSettings().setVpos(null)), "vpos configuration block is " +
                        "required"},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().position(1).setMaxBid(null)),
                        "Validation failed for argument [1] in public org.springframework.http.ResponseEntity<ru" +
                                ".yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad> ru.yandex" +
                                ".market.pricelabs.api.api.PublicAutostrategiesApi.autostrategyPost(java.lang" +
                                ".Integer,ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave,java" +
                                ".lang.String): [Field error in object 'autostrategySave' on field 'settings.vpos" +
                                ".maxBid': rejected value [null]; codes [NotNull.autostrategySave.settings.vpos" +
                                ".maxBid,NotNull.settings.vpos.maxBid,NotNull.maxBid,NotNull.java.lang.Long,NotNull];" +
                                " arguments [org.springframework.context.support.DefaultMessageSourceResolvable: " +
                                "codes [autostrategySave.settings.vpos.maxBid,settings.vpos.maxBid]; arguments []; " +
                                "default message [settings.vpos.maxBid]]; default message [must not be null]]"},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().position(1).setMaxBid(0L)),
                        valueMustBeString("maxBid", 1, true)},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().position(1).setMaxBid(-1L)),
                        valueMustBeString("maxBid", 1, true)},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().position(1).setMaxBid(84001L)),
                        valueMustBeString("maxBid", 8400, false)
                },
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().maxBid(45L).setPosition(null)),
                        "Validation failed for argument [1] in public org.springframework.http.ResponseEntity<ru" +
                                ".yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad> ru.yandex" +
                                ".market.pricelabs.api.api.PublicAutostrategiesApi.autostrategyPost(java.lang" +
                                ".Integer,ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave,java" +
                                ".lang.String): [Field error in object 'autostrategySave' on field 'settings.vpos" +
                                ".position': rejected value [null]; codes [NotNull.autostrategySave.settings.vpos" +
                                ".position,NotNull.settings.vpos.position,NotNull.position,NotNull.java.lang.Integer," +
                                "NotNull]; arguments [org.springframework.context.support" +
                                ".DefaultMessageSourceResolvable: codes [autostrategySave.settings.vpos.position," +
                                "settings.vpos.position]; arguments []; default message [settings.vpos.position]]; " +
                                "default message [must not be null]]"},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().maxBid(45L).setPosition(0)),
                        valueMustBeString("position", 1, true)},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().maxBid(45L).setPosition(-1)),
                        valueMustBeString("position", 1, true)},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().maxBid(45L).setPosition(11)),
                        valueMustBeString("position", 10, false)},
                {autostrategyInit(VPOS, auto -> {
                    auto.getSettings().getVpos().position(1).setMaxBid(1L);
                    auto.getFilter().setType(VENDOR);
                }), "models are required for filter type VENDOR"},
                {autostrategyInit(VPOS, auto -> {
                    auto.getSettings().getVpos().position(1).setMaxBid(1L);
                    auto.getFilter().setType(VENDOR);
                    auto.getFilter().setVendor(new AutostrategyFilterVendor());
                }), "models are required for filter type VENDOR"},
                {autostrategyInit(VPOS, auto -> {
                    auto.getSettings().getVpos().position(1).setMaxBid(1L);
                    auto.getFilter().setType(VENDOR);
                    auto.getFilter().setVendor(new AutostrategyFilterVendor().models(List.of()));
                }), "models are required for filter type VENDOR"},
                {autostrategyInit(VPOS, auto -> {
                    auto.getSettings().getVpos().position(1).setMaxBid(1L);
                    auto.getFilter().setType(VENDOR);
                    auto.getFilter().setVendor(new AutostrategyFilterVendor().models(Collections.singletonList(null)));
                }), "models are required for filter type VENDOR"},
                {autostrategyInit(CPA, auto -> auto.getSettings().getCpa().setDrrBid(0L)), valueMustBeString("drrBid"
                        , 50, true)},
                {autostrategyInit(CPA, auto -> auto.getSettings().getCpa().setDrrBid(10001L)), valueMustBeString(
                        "drrBid", 10000, false)},
        };
    }

    private static Object[][] validAutostrategies() {
        return new Object[][]{
                {autostrategyInit(CPO, auto -> auto.getFilter().getSimple().setPriceFrom(0L))},
                {autostrategyInit(CPO, auto -> auto.getFilter().getSimple().setPriceTo(0L))},
                {autostrategyInit(DRR, auto -> auto.getSettings().getDrr().setTakeRate(10000L))},
                {autostrategyInit(CPO, auto -> auto.getSettings().getCpo().setCpo(65535L))},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(30L))},
                {autostrategyInit(POS, auto -> auto.getSettings().getPos().position(1).setMaxBid(252000L))},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().position(1).setMaxBid(1L))},
                {autostrategyInit(VPOS, auto -> auto.getSettings().getVpos().position(1).setMaxBid(8400L))},
                {autostrategyInit(VPOS, auto -> {
                    auto.getSettings().getVpos().position(1).setMaxBid(1L);
                    auto.getFilter().setType(VENDOR);
                    auto.getFilter().setVendor(new AutostrategyFilterVendor().models(Collections.singletonList(1)));
                })},
                {autostrategyInit(CPA, auto -> auto.getSettings().getCpa().setDrrBid(50L))},
                {autostrategyInit(CPA, auto -> auto.getSettings().getCpa().setDrrBid(10000L))},
        };
    }

    private static Object[][] strategyStates() {
        TimingUtils.setTime(Utils.parseDateTimeAsInstant("2019-12-01T01:02:03"));
        var t1 = TimingUtils.getInstant();

        TimingUtils.addTime(10000);
        var t2 = TimingUtils.getInstant();

        final var enabled = true;
        final var disabled = false;

        // Все возможные комбинации
        return new Object[][]{
                // Дата стратегии, статус стратегии, дата линковки, статус линковки, ожидаемое состояние
                {t2, enabled, null, false, StateEnum.ACTIVATING},
                {t2, disabled, null, false, StateEnum.DEACTIVATING},

                {t1, enabled, t2, false, StateEnum.ACTIVATING},
                {t1, enabled, t2, true, StateEnum.ACTIVE},

                {t1, disabled, t2, false, StateEnum.INACTIVE},
                {t1, disabled, t2, true, StateEnum.DEACTIVATING},

                {t2, enabled, t1, false, StateEnum.ACTIVATING},
                {t2, enabled, t1, true, StateEnum.CHANGING},

                {t2, disabled, t1, false, StateEnum.CHANGING},
                {t2, disabled, t1, true, StateEnum.DEACTIVATING},

        };
    }

    static double toDrr(double value) {
        return new BigDecimal(value * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    void init(AutostrategyTarget target, Runnable initOnce) {
        publicApi = MockMvcProxy.buildProxy(PublicAutostrategiesApiInterfaces.class, publicApiBean);

        var table = target.get(
                coreTables.getAutostrategiesStateHistoryTable(),
                coreTables.getBlueAutostrategiesStateHistoryTable(),
                coreTables.getVendorBlueAutostrategiesStateHistoryTable());
        var search = target.get(whiteSearch, blueSearch, vendorBlueSearch);
        this.statesExecutor = target.get(
                executors.autostrategiesStateWhite(),
                executors.autostrategiesStateBlue(),
                executors.autostrategiesStateVendorBlue());

        this.autoTarget = target;
        this.target = target.name();

        cleanUpTables(table, search);
        YtScenarioExecutor.clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesEventTable()));

        this.offersExecutor = target.get(executors.offers(), executors.offersBlue());

        autostrategiesExecutor = target.get(executors.whiteAutostrategiesExecutor(),
                executors.blueAutostrategiesExecutor(), executors.vendorBlueAutostrategies());

        var virtualShopId = target.get(SHOP1, ApiConst.VIRTUAL_SHOP_BLUE);

        var shop = TmsTestUtils.shop(virtualShopId);
        shop.setStatus(ShopStatus.ACTIVE);

        var feeds = target.get(Set.of((long) FEED1, (long) FEED2), Set.of((long) ApiConst.VIRTUAL_FEED_BLUE));
        shop.setFeeds(feeds);
        this.metaProcessor = target.get(metaWhite, metaBlue, metaVendorBlue);

        testControls.initOnce(getClass(), () ->
                testControls.executeInParallel(
                        () -> testControls.saveShop(shop),
                        () -> {
                            if (virtualShopId != ApiConst.VIRTUAL_SHOP_BLUE) {
                                // Эти магазины нужны только для белого
                                Stream.of(SHOP2, SHOP3).forEach(shopId ->
                                        testControls.saveShop(TmsTestUtils.shop(shopId, s -> {
                                            s.setStatus(ShopStatus.ACTIVE);
                                            s.setFeeds(feeds);
                                        })));
                            }
                        },
                        initOnce,
                        getSaveOffersRunnable(target.get(false, true), SHOP1, SHOP2, offersExecutor)
                ));
    }

    public String getTarget() {
        return target;
    }

    PublicAutostrategiesApiInterfaces getPublicApi() {
        return publicApi;
    }

    @Test
    void testCreate() {
        var ret = create();
        assertEquals(List.of(ret), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));
    }

    @ParameterizedTest()
    @MethodSource("invalidAutostrategies")
    void testCreateInvalid(AutostrategySave auto, String expectError) {
        assertThrowsWithMessage(MockMvcProxyHttpException.class, () ->
                        checkResponse(publicApi.autostrategyPost(SHOP1, auto, target)),
                expectError(expectError));
    }

    @ParameterizedTest()
    @MethodSource("validAutostrategies")
    void testCreateValid(AutostrategySave auto) {
        checkResponse(publicApi.autostrategyPost(SHOP1, auto, target));
    }

    @Test
    void testWithState() {
        var ret = create();

        var linkedAt = timeSource().getInstant().with(ChronoField.MILLI_OF_SECOND, 0).plusMillis(9000);
        var linkedCount = 15367;
        final var linkedEnabled = true;

        statesExecutor.insert(List.of(autostrategyState(ret.getId(), SHOP1, state -> {
            state.setLinked_at(linkedAt);

            IntConsumer set = autoTarget.get(state::setLinked_count, state::setSsku_linked_count);
            set.accept(linkedCount);
            state.setLinked_enabled(linkedEnabled);
        })));

        // Для истории не подгружается linkedAt и linkedCount
        var history = checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret.getId(), target));
        assertEquals(List.of(ret), history);

        var load = checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target));

        // Ничего не изменилось в истории, но зато у нас появилась запись о
        ret.setLinkedAt(OffsetDateTime.ofInstant(linkedAt, Utils.getUtcZone()));
        ret.setOfferCount(linkedCount);
        ret.setLinkedEnabled(linkedEnabled);
        ret.setState(StateEnum.ACTIVE);
        assertEquals(ret, load);

    }

    @ParameterizedTest
    @MethodSource("strategyStates")
    void testWithDifferentStates(Instant timestamp, boolean enabled, Instant linkTimestamp, boolean linkedEnabled,
                                 StateEnum expectState) {
        TimingUtils.setTime(timestamp);
        var ret = create(enabled);

        if (linkTimestamp != null) {
            statesExecutor.insert(List.of(autostrategyState(ret.getId(), SHOP1, state -> {
                state.setLinked_at(linkTimestamp);
                state.setLinked_enabled(linkedEnabled);
            })));
        }
        if (linkTimestamp != null) {
            ret.setLinkedAt(OffsetDateTime.ofInstant(linkTimestamp, Utils.getUtcZone()));
            ret.setLinkedEnabled(linkedEnabled);
            ret.setOfferCount(0);
        }
        ret.setState(expectState);
        assertEquals(ret, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
    }

    @Test
    void testUpdate() {
        var ret = create();

        TimingUtils.addTime(101);

        var auto = autostrategy("s22", CPO);
        var ret2 = checkResponse(publicApi.autostrategyPut(SHOP1, ret.getId(), auto, target));

        log.info("Autostrategy: {}", ret2);

        assertEquals(ret2, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret2), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));
        assertEquals(restoreSave(auto), transformLoad(ret2));
    }

    @Test
    void testUpdateDefaultFromTo() {
        var ret = create();

        TimingUtils.addTime(101);

        var auto = autostrategy("s22", CPO);
        auto.getFilter().getSimple().setPriceFrom(null);
        auto.getFilter().getSimple().setPriceTo(null);

        var ret2 = checkResponse(publicApi.autostrategyPut(SHOP1, ret.getId(), auto, target));

        log.info("Autostrategy: {}", ret2);

        assertEquals(ret2, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret2), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        assertEquals(restoreSave(auto), transformLoad(ret2));
    }

    @Test
    void testUpdateDRR() {
        var ret = create();

        TimingUtils.addTime(101);

        var auto = autostrategy("s22", DRR);

        var ret2 = checkResponse(publicApi.autostrategyPut(SHOP1, ret.getId(), auto, target));

        log.info("Autostrategy: {}", ret2);

        assertEquals(ret2, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret2), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        // Осталось с предыдущего сохранения (из метода create)
        auto.getFilter().setVendor(ret.getFilter().getVendor());
        auto.getSettings().setCpo(ret.getSettings().getCpo());
        auto.getSettings().setPos(ret.getSettings().getPos());
        auto.getSettings().setVpos(ret.getSettings().getVpos());
        auto.getSettings().setCpa(ret.getSettings().getCpa());
        assertEquals(auto, transformLoad(ret2));
    }

    @Test
    void testCreateDRR() {
        var auto = autostrategy("s1", DRR);
        var ret = checkResponse(publicApi.autostrategyPost(SHOP1, auto, target));

        log.info("Autostrategy: {}", ret);
        assertEquals(ret, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret.getId(), target)));

        assertEquals(restoreSave(auto), transformLoad(ret));
    }

    @Test
    void testUpdatePOS() {
        var ret = create();

        TimingUtils.addTime(101);

        var auto = autostrategy("s22", POS);

        var ret2 = checkResponse(publicApi.autostrategyPut(SHOP1, ret.getId(), auto, target));

        log.info("Autostrategy: {}", ret2);

        assertEquals(ret2, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret2), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        // Осталось с предыдущего сохранения (из метода create)
        auto.getFilter().setVendor(ret.getFilter().getVendor());
        auto.getSettings().setCpo(ret.getSettings().getCpo());
        auto.getSettings().setDrr(ret.getSettings().getDrr());
        auto.getSettings().setVpos(ret.getSettings().getVpos());
        auto.getSettings().setCpa(ret.getSettings().getCpa());
        assertEquals(auto, transformLoad(ret2));
    }

    @Test
    void testCreatePOS() {
        var auto = autostrategy("s1", POS);
        var ret = checkResponse(publicApi.autostrategyPost(SHOP1, auto, target));

        log.info("Autostrategy: {}", ret);
        assertEquals(ret, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret.getId(), target)));

        assertEquals(restoreSave(auto), transformLoad(ret));
    }

    @Test
    void testUpdateVPOS() {
        var ret = create();

        TimingUtils.addTime(101);

        var auto = autostrategy("s22", VENDOR, VPOS);

        var ret2 = checkResponse(publicApi.autostrategyPut(SHOP1, ret.getId(), auto, target));

        log.info("Autostrategy: {}", ret2);

        assertEquals(ret2, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret2), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        // Осталось с предыдущего сохранения (из метода create)
        auto.getFilter().setSimple(ret.getFilter().getSimple());
        auto.getSettings().setCpo(ret.getSettings().getCpo());
        auto.getSettings().setDrr(ret.getSettings().getDrr());
        auto.getSettings().setPos(ret.getSettings().getPos());
        auto.getSettings().setCpa(ret.getSettings().getCpa());
        assertEquals(auto, transformLoad(ret2));
    }

    @Test
    void testCreateVPOS() {
        var auto = autostrategy("s1", VENDOR, VPOS);
        var ret = checkResponse(publicApi.autostrategyPost(SHOP1, auto, target));

        log.info("Autostrategy: {}", ret);
        assertEquals(ret, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret.getId(), target)));

        assertEquals(restoreSave(auto), transformLoad(ret));
    }

    @Test
    void testDelete() {
        var ret = create();
        checkResponse(publicApi.autostrategyDelete(SHOP1, ret.getId(), target));

        assertThrowsWithMessage(MockMvcProxyHttpException.class,
                () -> publicApi.autostrategyGet(SHOP1, ret.getId(), target),
                "Unable to load autostrategy with " + ret.getId() + ": no records");
    }

    @Test
    void testHistory() {
        var ret1 = create();
        var ret2 = create();
        assertEquals(List.of(ret2, ret1), checkResponse(publicApi.autostrategiesGet(SHOP1, target)));
        assertEquals(List.of(), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, Integer.MAX_VALUE - 1, target)));

        TimingUtils.addTime(12000);
        var v11 = autostrategy("s22", CPO);
        v11.getFilter().getSimple().setVendors(List.of());
        var ret11 = checkResponse(publicApi.autostrategyPut(SHOP1, ret1.getId(), v11, target));

        TimingUtils.addTime(13000);
        var v12 = autostrategy("s23", CPO);
        v12.getFilter().getSimple().setCategories(List.of(5L));
        var ret12 = checkResponse(publicApi.autostrategyPut(SHOP1, ret1.getId(), v12, target));

        TimingUtils.addTime(14000);
        var ret13 = checkResponse(publicApi.autostrategyPut(SHOP1, ret1.getId(),
                autostrategy("s23", DRR), target));

        var ret13Copy = checkResponse(publicApi.autostrategyGet(SHOP1, ret1.getId(), target));
        assertEquals(ret13, ret13Copy);

        assertEquals(List.of(ret13, ret12, ret11, ret1),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret1.getId(), target)));

        assertEquals(List.of(ret2), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret2.getId(), target)));

        TimingUtils.addTime(15000);
        checkResponse(publicApi.autostrategyDelete(SHOP1, ret1.getId(), target));

        ret13Copy.setTimestamp(ret13Copy.getTimestamp().plus(15000, ChronoUnit.MILLIS));
        assertEquals(List.of(ret13Copy, ret13, ret12, ret11, ret1),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret1.getId(), target)));

    }

    @Test
    void testReorder() {
        var ret1 = create();
        var ret2 = create();

        var orig = checkResponse(publicApi.autostrategiesGet(SHOP1, target));
        assertEquals(List.of(ret2, ret1), orig);

        TimingUtils.addTime(10000);
        var ret21 = checkResponse(publicApi.autostrategiesReorderPost(SHOP1,
                reorder(Map.of(ret1.getId(), 101, ret2.getId(), 102)), target));

        Runnable addTime = () -> {
            ret1.setTimestamp(ret1.getTimestamp().plus(10000, ChronoUnit.MILLIS));
            ret2.setTimestamp(ret2.getTimestamp().plus(10000, ChronoUnit.MILLIS));
        };

        ret1.setPriority(101);
        ret2.setPriority(102);
        addTime.run();
        assertEquals(List.of(ret2, ret1), ret21);
        assertEquals(ret21, checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        TimingUtils.addTime(10000);
        var ret12 = checkResponse(publicApi.autostrategiesReorderPost(SHOP1,
                reorder(Map.of(ret1.getId(), 102, ret2.getId(), 101)), target));
        ret1.setPriority(102);
        ret2.setPriority(101);
        addTime.run();
        assertEquals(List.of(ret1, ret2), ret12);
        assertEquals(ret12, checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        TimingUtils.addTime(10000);
        var ret21Same = checkResponse(publicApi.autostrategiesReorderPost(SHOP1,
                reorder(Map.of(ret1.getId(), 100, ret2.getId(), 100)), target));
        ret1.setPriority(100);
        ret2.setPriority(100);
        addTime.run();
        assertEquals(List.of(ret2, ret1), ret21Same);
        assertEquals(ret21Same, checkResponse(publicApi.autostrategiesGet(SHOP1, target)));

        assertEquals(List.of(ret21Same.get(1), ret12.get(0), ret21.get(1), orig.get(1)),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret1.getId(), target)));

        assertEquals(List.of(ret21Same.get(0), ret12.get(1), ret21.get(0), orig.get(0)),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret2.getId(), target)));
    }

    @Test
    void testGetOffersEmpty() {
        assertEquals(List.of(),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP3,
                        new AutostrategiesOfferFilter(), 1, 1, target)));
    }

    @Test
    void testGetOffersUnknownShop() {
        assertEquals(List.of(),
                checkResponse(publicApi.autostrategiesOffersPost(UNKNOWN_SHOP,
                        new AutostrategiesOfferFilter(), 1, 1, target)));
    }

    @Test
    void testGetOfferCountEmpty() {
        assertEquals(new AutostrategiesOfferCount().offerCount(0),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP3, new AutostrategiesOfferFilter(), target)));
    }

    @Test
    void testGetOfferCountUnknownShop() {
        assertEquals(new AutostrategiesOfferCount().offerCount(0),
                checkResponse(publicApi.autostrategiesOfferCountPost(UNKNOWN_SHOP,
                        new AutostrategiesOfferFilter(), target)));
    }

    @Test
    void testGetOfferCount() {
        // Только 6 со стратегиями
        assertEquals(new AutostrategiesOfferCount().offerCount(6),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1, new AutostrategiesOfferFilter(), target)));
    }

    @Test
    void testGetOfferCountForAutostrategies() {
        assertEquals(new AutostrategiesOfferCount().offerCount(5),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(List.of(3, 4)), target)));
    }

    @Test
    void testGetOfferCountForAutostrategy() {
        assertEquals(new AutostrategiesOfferCount().offerCount(4),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(List.of(3)), target)));
    }

    @Test
    void testGetOfferCountForPrice() {
        assertEquals(new AutostrategiesOfferCount().offerCount(3),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(List.of(3))
                                .priceFrom(1L)
                                .priceTo(1000L), target)));
    }

    @Test
    void testGetOfferCountForAutostrategiesList() {
        var sample = offersSampleShop1();
        assertEquals(new AutostrategiesOfferCount().offerCount(1),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(
                                        List.of(4)
                                ),
                        target)));
    }

    @Test
    void testGetOfferCountForCampaignsList() {
        var sample = offersSampleShop1();
        assertEquals(new AutostrategiesOfferCount().offerCount(1),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .campaignIdList(
                                        List.of(5L)
                                ),
                        target)));
    }

    @Test
    void testGetOfferCountForAutostrategiesAndCampaignsList() {
        var sample = offersSampleShop1();
        assertEquals(new AutostrategiesOfferCount().offerCount(2),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(
                                        List.of(4)
                                )
                                .campaignIdList(
                                        List.of(5L)
                                ),
                        target)));
    }

    @Test
    void testGetOfferCountForAutostrategiesWithout() {
        var sample = offersSampleShop1();
        assertEquals(new AutostrategiesOfferCount().offerCount(1),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyType(AutostrategyTypeEnum.WITHOUT),
                        target)));
    }

    @Test
    void testGetOfferCountForAutostrategiesIgnore() {
        var sample = offersSampleShop1();
        assertEquals(new AutostrategiesOfferCount().offerCount(7),
                checkResponse(publicApi.autostrategiesOfferCountPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyType(AutostrategyTypeEnum.IGNORE),
                        target)));
    }

    @Test
    void testGetOffers() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(0), sample.get(1), sample.get(2), sample.get(3), sample.get(4),
                        sample.get(6))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter(), null, null, target)));
    }

    @Test
    void testGetOffersSingle() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(0))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter(), 1, 1, target)));
    }

    @Test
    void testGetOffersPage12() {
        var sample = offersSampleShop1();
        assertEquals(map(sample.subList(0, 2)),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter(), 1, 2, target)));
    }

    @Test
    void testGetOffersPage22() {
        var sample = offersSampleShop1();
        assertEquals(map(sample.subList(2, 4)),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter(), 2, 2, target)));
    }

    @Test
    void testGetOffersPage23() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(4), sample.get(6))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter(), 3, 2, target)));
    }

    @Test
    void testGetOffersPage24() {
        var sample = offersSampleShop1();
        assertEquals(List.of(),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter(), 4, 2, target)));
    }

    @Test
    void testGetOffersForAutostrategiesList() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(4))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(
                                        List.of(4)
                                ),
                        null, null, target)));
    }

    @Test
    void testGetOffersForCampaignsList() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(6))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .campaignIdList(
                                        List.of(5L)
                                ),
                        null, null, target)));
    }

    @Test
    void testGetOffersForAutostrategiesAndCampaignsList() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(4), sample.get(6))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyIdList(
                                        List.of(4)
                                )
                                .campaignIdList(
                                        List.of(5L)
                                ),
                        null, null, target)));
    }

    @Test
    void testGetOffersForAutostrategiesWithout() {
        var sample = offersSampleShop1();
        assertEquals(map(List.of(sample.get(5))),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyType(AutostrategyTypeEnum.WITHOUT),
                        null, null, target)));
    }

    @Test
    void testGetOffersForAutostrategiesIgnore() {
        var sample = offersSampleShop1();
        assertEquals(map(sample),
                checkResponse(publicApi.autostrategiesOffersPost(SHOP1,
                        new AutostrategiesOfferFilter()
                                .autostrategyType(AutostrategyTypeEnum.IGNORE),
                        null, null, target)));
    }

    @Test
    void testEstimateOfferCountEmpty() {
        assertEquals(new AutostrategiesOfferCount().offerCount(0),
                checkResponse(publicApi.autostrategyEstimateOfferCountPost(SHOP3, null, target, null)));
    }

    //

    @Test
    void testEstimateOfferCountUnknownShop() {
        assertEquals(new AutostrategiesOfferCount().offerCount(0),
                checkResponse(publicApi.autostrategyEstimateOfferCountPost(UNKNOWN_SHOP, null, target, null)));
    }

    @Test
    void testEstimateOfferCount() {
        assertEquals(new AutostrategiesOfferCount().offerCount(6),
                checkResponse(publicApi.autostrategyEstimateOfferCountPost(SHOP2, null, target, null)));
    }

    @Test
    void testEstimateOfferCountForPrice() {
        assertEquals(new AutostrategiesOfferCount().offerCount(3),
                checkResponse(publicApi.autostrategyEstimateOfferCountPost(SHOP2,
                        new AutostrategyFilter()
                                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                                .simple(new AutostrategyFilterSimple()
                                        .priceFrom(1000L)), target, null)));
    }

    @Test
    void testEstimateOfferCountWithAdvCampaignId() {
        ((SequenceServiceImpl) sequenceService).resetSequences();
        autostrategiesExecutor.clearTargetTable();
        // Приоритет равен id
        metaProcessor.create(1, SHOP1, autostrategy("id1", CPA));
        metaProcessor.create(1, SHOP1, autostrategy("id2", CPA));
        metaProcessor.create(1, SHOP1, autostrategy("id3", CPA));
        metaProcessor.create(1, SHOP1, autostrategy("id4", CPA)); // Оффер с этой стратегией не будет выбран
        assertEquals(new AutostrategiesOfferCount().offerCount(5),
                checkResponse(publicApi.autostrategyEstimateOfferCountPost(SHOP1, null, target, 3L)));
    }

    @Test
    void testBatchPost() {

        var auto0 = autostrategy("s0", CPO);
        var auto1 = autostrategy("s1", DRR);
        var rets = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(auto0),
                new AutostrategySaveWithId().autostrategy(auto1)), target, null, null));

        log.info("Autostrategy: {}", rets);

        assertEquals(2, rets.size());
        var ret0 = rets.get(0);
        var ret1 = rets.get(1);

        for (var r : rets) {
            assertEquals(r, checkResponse(publicApi.autostrategyGet(SHOP1, r.getId(), target)));
            assertEquals(List.of(r), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, r.getId(), target)));
        }

        assertEquals(rets, checkResponse(publicApi.autostrategyBatchGet(SHOP1, toAutostrategyId(rets), target)));

        assertEquals(restoreSave(auto0), transformLoad(ret0));
        assertEquals(restoreSave(auto1), transformLoad(ret1));

        TimingUtils.addTime(101);

        var auto20 = autostrategy("s00", CPO);
        var auto2 = autostrategy("s2", VENDOR, VPOS);
        var auto21 = autostrategy("s11", DRR);
        var rets2 = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().id(ret0.getId()).autostrategy(auto20),
                new AutostrategySaveWithId().autostrategy(auto2),
                new AutostrategySaveWithId().id(ret1.getId()).autostrategy(auto21)), target, null, null));

        log.info("Autostrategy: {}", rets2);

        assertEquals(3, rets2.size());
        var ret20 = rets2.get(0);
        var ret2 = rets2.get(1);
        var ret21 = rets2.get(2);

        assertEquals(ret20,
                checkResponse(publicApi.autostrategyGet(SHOP1, ret20.getId(), target)));
        assertEquals(List.of(ret20, ret0),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret20.getId(), target)));

        assertEquals(ret2,
                checkResponse(publicApi.autostrategyGet(SHOP1, ret2.getId(), target)));
        assertEquals(List.of(ret2),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret2.getId(), target)));

        assertEquals(ret21,
                checkResponse(publicApi.autostrategyGet(SHOP1, ret21.getId(), target)));
        assertEquals(List.of(ret21, ret1),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret21.getId(), target)));

        assertEquals(rets2, checkResponse(publicApi.autostrategyBatchGet(SHOP1, toAutostrategyId(rets2), target)));
        assertEquals(restoreSave(auto20), transformLoad(rets2.get(0)));
        assertEquals(restoreSave(auto2), transformLoad(rets2.get(1)));
        assertEquals(restoreSave(auto21), transformLoad(rets2.get(2)));
    }

    @Test
    void testBatchPostNotFound() {
        var rets = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().id(111222).autostrategy(autostrategy("s0", CPO)),
                new AutostrategySaveWithId().id(222111).autostrategy(autostrategy("s1", DRR))), target, null, null));
        assertEquals(2, rets.size());
        rets.forEach(auto -> assertEquals(PublicAutostrategiesApi.ERROR_STRATEGY_NOT_FOUND, auto.getError()));
    }

    @Test
    void testBatchDeleteAll() {
        var auto0 = autostrategy("s0", CPO);
        var auto1 = autostrategy("s1", DRR);
        var rets = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(auto0),
                new AutostrategySaveWithId().autostrategy(auto1)), target, null, null));

        log.info("Autostrategy: {}", rets);

        checkResponse(publicApi.autostrategyBatchDelete(SHOP1, toAutostrategyId(rets), target));

        assertEquals(List.of(), checkResponse(publicApi.autostrategyBatchGet(SHOP1, toAutostrategyId(rets), target)));
    }

    @Test
    void testBatchDeleteSingle() {
        var auto0 = autostrategy("s0", CPO);
        var auto1 = autostrategy("s1", DRR);
        var rets = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(auto0),
                new AutostrategySaveWithId().autostrategy(auto1)), target, null, null));

        log.info("Autostrategy: {}", rets);

        assertEquals(2, rets.size());

        var ret0 = rets.get(0);
        var ret1 = rets.get(1);

        checkResponse(publicApi.autostrategyBatchDelete(SHOP1, List.of(ret0.getId()), target));

        assertEquals(List.of(ret1), checkResponse(publicApi.autostrategyBatchGet(SHOP1,
                toAutostrategyId(rets), target)));
    }

    @Test
    void testBatchPostAndDeleteUnknown() {

        var auto0 = autostrategy("s0", CPO);
        var auto1 = autostrategy("s1", DRR);
        var rets = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().autostrategy(auto0),
                new AutostrategySaveWithId().autostrategy(auto1)), target, null, null));

        log.info("Autostrategy: {}", rets);

        assertEquals(2, rets.size());

        var ret0 = rets.get(0);
        var ret1 = rets.get(1);

        checkResponse(publicApi.autostrategyBatchDelete(SHOP1, List.of(ret0.getId()), target));

        var auto20 = autostrategy("s00", CPO);
        var auto21 = autostrategy("s11", DRR);
        var rets2 = checkResponse(publicApi.autostrategyBatchPost(SHOP1, List.of(
                new AutostrategySaveWithId().id(ret0.getId()).autostrategy(auto20),
                new AutostrategySaveWithId().id(ret1.getId()).autostrategy(auto21)), target, null, null));

        log.info("Autostrategy: {}", rets2);
        assertEquals(2, rets2.size());

        var ret20 = rets2.get(0);
        assertEquals(PublicAutostrategiesApi.ERROR_STRATEGY_NOT_FOUND, ret20.getError());

        var ret21 = rets2.get(1);
        assertEquals(ret21, checkResponse(publicApi.autostrategyGet(SHOP1, ret21.getId(), target)));
        assertEquals(List.of(ret21, ret1),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret21.getId(), target)));


        checkResponse(publicApi.autostrategyBatchDelete(SHOP1, List.of(ret0.getId()), target));
        assertEquals(List.of(ret21),
                checkResponse(publicApi.autostrategyBatchGet(SHOP1, toAutostrategyId(rets), target)));
    }

    @Test
    void testChangeState() {
        var ret1 = create();
        var ret2 = create();

        var orig = checkResponse(publicApi.autostrategiesGet(SHOP1, target));
        assertEquals(List.of(ret2, ret1), orig);

        TimingUtils.addTime(10000);
        publicApi.autostrategyBatchChangeStatePost(SHOP1, List.of(ret1.getId(), ret2.getId(), 100500), false, target);

        Runnable addTime1 = () -> ret1.setTimestamp(ret1.getTimestamp().plus(10000, ChronoUnit.MILLIS));
        Runnable addTime2 = () -> ret2.setTimestamp(ret2.getTimestamp().plus(10000, ChronoUnit.MILLIS));

        ret1.setEnabled(false);
        ret1.setState(StateEnum.DEACTIVATING);
        ret2.setEnabled(false);
        ret2.setState(StateEnum.DEACTIVATING);
        addTime1.run();
        addTime2.run();
        var step1 = checkResponse(publicApi.autostrategiesGet(SHOP1, target));
        assertEquals(List.of(ret2, ret1), step1);

        TimingUtils.addTime(10000);
        publicApi.autostrategyBatchChangeStatePost(SHOP1, List.of(ret1.getId()), true, target);
        ret1.setEnabled(true);
        ret1.setState(StateEnum.ACTIVATING);
        addTime1.run();
        var step2 = checkResponse(publicApi.autostrategiesGet(SHOP1, target));
        assertEquals(List.of(ret2, ret1), step2);

        TimingUtils.addTime(10000);
        publicApi.autostrategyBatchChangeStatePost(SHOP1, List.of(ret1.getId(), ret2.getId()), false, target);
        ret1.setEnabled(false);
        ret1.setState(StateEnum.DEACTIVATING);
        addTime1.run();
        var step3 = checkResponse(publicApi.autostrategiesGet(SHOP1, target));
        assertEquals(List.of(ret2, ret1), step3);

        assertEquals(List.of(step3.get(1), step2.get(1), step1.get(1), orig.get(1)),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret1.getId(), target)));

        assertEquals(List.of(step1.get(0), orig.get(0)),
                checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret2.getId(), target)));
    }

    @Override
    public String expectError(String message) {
        var messageWithTarget = message.replaceAll("(.+&autoStrategyTarget=)(" + allTargets + ")(.*)",
                "$1" + target + "$3");
        return super.expectError(messageWithTarget);
    }

    private List<AutostrategyOffer> map(List<Offer> offers) {
        return offers.stream()
                .map(offer -> new AutostrategyOffer()
                        .feedId(offer.getFeed_id())
                        .offerId(offer.getOffer_id())
                        .autostrategyId(
                                offer.getApp_autostrategy_id_int() > 0
                                        ? offer.getApp_autostrategy_id_int()
                                        : (int) offer.getApp_strategy_id_long()
                        )
                        .modelId(offer.isCard() ? offer.getModel_id() : null)
                        .price(offer.getPrice())
                        .title(offer.getName())
                        .type(
                                offer.getApp_autostrategy_id_int() > 0
                                        ? AutostrategyOffer.TypeEnum.AUTOSTRATEGY
                                        :
                                        offer.getStrategy_type() == StrategyType.CAMPAIGN
                                                ? AutostrategyOffer.TypeEnum.CAMPAIGN
                                                : AutostrategyOffer.TypeEnum.EMPTY
                        ).origPrice(0.0)
                        .recommendedPrice(0.0)
                        .recommendedPromocode(0.0)
                        .priceStatus(-1)
                )
                .collect(Collectors.toList());
    }

    Map<Integer, List<Offer>> offersSampleShop1GroupedByCategories() {
        return offersSampleShop1().stream().collect(Collectors.groupingBy(o -> (int) o.getCategory_id()));
    }

    List<Offer> offersSampleShop2NoAutostrategy() {
        return offersSampleShopNoAutostrategy(autoTarget.get(false, true), SHOP2);
    }

    List<Offer> offersSampleShop1() {
        return offersSampleShop(autoTarget.get(false, true), SHOP1);
    }
    //CHECKSTYLE:ON

    Map<Integer, BlueBidsRecommendation> getBlueBidsRecommendations() {
        return Map.of(
                CATEGORY_DEFAULT, new BlueBidsRecommendation(CATEGORY_DEFAULT, 0.5, 0.1, 0.22, getInstant()),
                CATEGORY1, new BlueBidsRecommendation(CATEGORY1, 0.6, 0.2, 0.33, getInstant()),
                CATEGORY2, new BlueBidsRecommendation(CATEGORY2, 0.7, 0.3, 0.44, getInstant())
        );
    }

    private AutostrategyLoad create() {
        return create(true);
    }

    private AutostrategyLoad create(boolean enabled) {
        var auto = autostrategy("s1", CPO);
        auto.setEnabled(enabled);
        var ret = checkResponse(publicApi.autostrategyPost(SHOP1, auto, target));

        log.info("Autostrategy: {}", ret);
        assertEquals(ret, checkResponse(publicApi.autostrategyGet(SHOP1, ret.getId(), target)));
        assertEquals(List.of(ret), checkResponse(publicApi.autostrategyHistoryGet(SHOP1, ret.getId(), target)));

        assertEquals(restoreSave(auto), transformLoad(ret));

        return ret;
    }

    AutostrategyFilter autostrategyFilter(List<String> offerIds) {
        return new AutostrategyFilter()
                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                .simple(new AutostrategyFilterSimple()
                        .offerIds(offerIds));
    }

    private void cleanUpTables(BindingTable<AutostrategyStateHistory> table, AutostrategiesSearch search) {
        AbstractAutostrategiesMetaProcessorTest.cleanupTables(search.getCfg(), search.getHistoryCfg(),
                search.getStateCfg(),
                ytCfg.getProcessorCfg(table),
                search.getFilterCfg(), search.getFilterHistoryCfg(),
                testControls);
    }
}
