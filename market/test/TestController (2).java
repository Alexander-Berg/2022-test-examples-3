package ru.yandex.market.mboc.app.test;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.mboc.app.security.SecuredRoles;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.exceptions.BadUserRequestException;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.CutoffState;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.Key;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoffInfo;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferUtils;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.mail.EmailService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.upload.OfferUploadQueueService;
import ru.yandex.market.mboc.common.users.UserRoles;
import ru.yandex.market.springmvctots.annotations.TsIgnore;

@TsIgnore
@RequestMapping("/api/dev")
@SecuredRoles(UserRoles.DEVELOPER)
@RestController
public class TestController {
    private final ModelStorageCachingService modelStorageCachingService;
    private final EmailService emailService;
    private final List<String> catmanEmails;
    private final OfferCutoffService cutoffService;
    private final OfferRepository offerRepository;
    private final Collection<OfferUploadQueueService> offerUploadQueueServices;
    private GoldenMatrixService goldenMatrixService;
    private UltraControllerService ultraControllerService;
    private NamedParameterJdbcTemplate jdbc;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TestController(
            @Lazy GoldenMatrixService goldenMatrixService,
            UltraControllerService ultraControllerService,
            NamedParameterJdbcTemplate jdbc,
            ModelStorageCachingService modelStorageCachingService,
            EmailService emailService,
            @Value("${mboc.email.cat-man.to}") List<String> catmanEmails,
            OfferCutoffService cutoffService,
            OfferRepository offerRepository,
            Collection<OfferUploadQueueService> offerUploadQueueServices
    ) {
        this.goldenMatrixService = goldenMatrixService;
        this.ultraControllerService = ultraControllerService;
        this.jdbc = jdbc;
        this.modelStorageCachingService = modelStorageCachingService;
        this.emailService = emailService;
        this.catmanEmails = catmanEmails;
        this.cutoffService = cutoffService;
        this.offerRepository = offerRepository;
        this.offerUploadQueueServices = offerUploadQueueServices;
    }

    @GetMapping
    public Map<String, Object> index() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("some", "thing");
        return map;
    }

    @GetMapping("/golden-size")
    public long goldenSize() {
        return goldenMatrixService.getMatrixSize();
    }

    @GetMapping(value = "/test-uc", produces = "text/plain")
    public String testUC() {
        UltraController.Offer offer = UltraController.Offer.newBuilder()
            .setOffer("SCF690/17 Бутылочка из полипропилена (125 мл, 0мес+) Philips Avent. Серия Natural")
            .setShopCategoryName("Все товары/Детские товары/Товары для мам и малышей/Кормление/Бутылочки и ниблеры")
            .setVendorCode("Philips Avent")
            .setSkuShop("SCF690/17")
            .setReturnMarketNames(true)
            .build();
        UltraController.EnrichedOffer enrichedOffer = ultraControllerService.enrichSingleOffer(offer);
        return enrichedOffer.toString();
    }

    @GetMapping(value = "/pg-activity")
    public List<ActivityRecord> checkTables() {
        return jdbc.query("select * from pg_stat_activity", rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columns = metaData.getColumnCount();
            ArrayList<ActivityRecord> result = new ArrayList<>();
            while (rs.next()) {
                HashMap<String, Object> map = new HashMap<>();
                for (int i = 1; i < columns; i++) {
                    map.put(metaData.getColumnName(i), rs.getObject(i));
                }
                result.add(new ActivityRecord(map));
            }
            return result;
        });
    }

    @SecuredRoles({})
    @GetMapping("/get-model")
    public Model getModel(@RequestParam("id") long id) {
        return modelStorageCachingService.getModelsFromMboThenPg(Collections.singleton(id)).get(id);
    }

    @GetMapping("/force-upload-offer")
    public String forceUploadOffer(@RequestParam List<Long> ids) {
        List<Offer> offers = offerRepository.getOffersByIds(ids);
        Map<Long, Model> mskus =
            modelStorageCachingService.getModelsFromMboThenPg(OfferUtils.collectMarketSkus(offers));
        offers.forEach(offer -> {
            if (!offer.hasApprovedSkuMapping()) {
                throw new BadUserRequestException("Offer " + offer.getId() + " doesn't have approved mapping");
            }
            long mskuId = offer.getApprovedSkuMapping().getMappingId();
            Model model = mskus.get(mskuId);
            if (model == null) {
                throw new BadUserRequestException("Can't find model " + mskuId + " for offer " + offer.getId());
            }

            if (!model.isPublishedOnBlueMarket()) {
                throw new BadUserRequestException("Model " + mskuId
                    + " is not published on blue market for offer " + offer.getId());
            }
        });

        offerUploadQueueServices.forEach(s -> s.enqueueNow(offers));
        return "OK";
    }

    @SecuredRoles({})
    @GetMapping(value = "/tanker-codes", produces = "application/json")
    public List<TankerCode> tankerCodes() {
        List<TankerCode> result = MbocErrors.getAllTemplates().stream()
            .map(template -> new TankerCode(template.code(), template.message()))
            .collect(Collectors.toList());

        result.addAll(Stream.of(ContentCommentType.values())
            .map(type -> new TankerCode(type.getMessageCode(), type.getDescription()))
            .collect(Collectors.toList()));
        return result;
    }

    @SecuredRoles({})
    @GetMapping(value = "/open-cutoff", produces = "application/json")
    public OfferCutoff openCutoff(WebRequest request) {
        Map<String, List<String>> params = getParams(request);
        String shopSku = params.get("shopSku").get(0);
        int supplierId = Integer.parseInt(params.get("supplierId").get(0));
        String typeId = params.get("typeId").get(0);
        CutoffState state = CutoffState.OPEN;

        params.remove("shopSku");
        params.remove("supplierId");
        params.remove("typeId");
        params.remove("state");

        OfferCutoffInfo cutoffInfo = TestControllerUtils.generateOfferCutoffInfoByTypeId(typeId);

        OfferCutoff cutoff = new OfferCutoff()
            .setShopSku(shopSku)
            .setSupplierId(supplierId)
            .setOfferCutoffInfo(cutoffInfo)
            .setState(state);

        return cutoffService.openCutoff(cutoff).orElse(null);
    }

    @SecuredRoles({})
    @GetMapping(value = "/close-cutoff")
    public String closeCutoff(WebRequest request) {
        Map<String, List<String>> params = getParams(request);
        String shopSku = params.get("shopSku").get(0);
        int supplierId = Integer.parseInt(params.get("supplierId").get(0));
        String typeId = params.get("typeId").get(0);
        if (!cutoffService.closeCutoff(new Key(supplierId, shopSku, typeId)).isPresent()) {
            return "No open cut-off with specified params exist.";
        }
        return "Cut-off marked as closed.";
    }

    @GetMapping("/email/cat-man-new-offers")
    public boolean emailCatManNewOffers() {
        return emailService.mailCatMansAboutNewOffers(catmanEmails, TestControllerUtils.getCatManNewOffersData());
    }

    @GetMapping("/email/cat-man-need-action")
    public boolean emailCatManNeedAction() {
        return emailService.mailCatMansAboutCategoryOffersNeedAction(catmanEmails,
            TestControllerUtils.getCatManNeedActionData());
    }

    @GetMapping("/templated/{something}/yay")
    public String testTemplated(@PathVariable("something") String something) {
        return "Hello " + something;
    }

    @GetMapping("/suffixed/**")
    public String suffixed() {
        return "Suffixed";
    }

    private Map<String, List<String>> getParams(WebRequest request) {
        Map<String, String[]> rawParams = request.getParameterMap();
        Map<String, List<String>> params = new HashMap<>();
        rawParams.forEach((key, values) -> params.put(key, Arrays.asList(values)));
        return params;
    }

    /**
     * Хак, чтобы сваггер не ругался на типы. Потом выпилим весь этот контроллер.
     */
    public static class ActivityRecord {
        final Map<String, Object> activity;

        ActivityRecord(Map<String, Object> activity) {
            this.activity = activity;
        }

        public Map<String, Object> getActivity() {
            return activity;
        }
    }

    public static class TankerCode {
        private final String code;
        private final String defaultMessage;

        public TankerCode(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() {
            return code;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
}
