package ru.yandex.market.clab.tms.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.clab.common.cache.CachedParamInfo;
import ru.yandex.market.clab.common.cache.CategoryInfoCache;
import ru.yandex.market.clab.common.config.component.ServicesConfig;
import ru.yandex.market.clab.common.config.http.HttpClientConfig;
import ru.yandex.market.clab.common.config.nas.NasConfig;
import ru.yandex.market.clab.common.db.good.CategoryUtils;
import ru.yandex.market.clab.common.mbo.data.form.EditorBlock;
import ru.yandex.market.clab.common.mbo.data.form.EditorForm;
import ru.yandex.market.clab.common.mbo.data.form.EditorTab;
import ru.yandex.market.clab.common.service.category.CategoryRepository;
import ru.yandex.market.clab.common.service.category.CategoryService;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.nas.PhotoService;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepository;
import ru.yandex.market.clab.common.utils.GoodMskuHierarchy;
import ru.yandex.market.clab.db.jooq.generated.enums.CleanupStatus;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.test.BaseIntegrationTest;
import ru.yandex.market.clab.tms.service.ocr.data.FullText;
import ru.yandex.market.clab.tms.service.ocr.data.RecognizeResponse;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.FormalizerServiceStub;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelEdit;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest(classes = {
    HttpClientConfig.class,
    ServicesConfig.class,
    NasConfig.class
})
@Ignore
public class OcrTest extends BaseIntegrationTest {

    private Logger log = LogManager.getLogger();

    private static final String OUTPUT_FILE = "C:\\Development\\tech\\output.csv";
    private static final String OUTPUT_PHOTOS = "C:\\Development\\tech\\output_photos.csv";
    private static final String OUTPUT_PARAMS = "C:\\Development\\tech\\output_params.csv";

    @Value("${contentlab.ocr.path:https://api-translate.ocr.yandex.net}")
    private String ocrPath;

    @Value("${contentlab.ocr.apiKey}")
    private String apiKey;

    @Value("${market.formalizer.host}")
    private String formalizerHost;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private EditedPhotoRepository editedPhotoRepository;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    private CategoryInfoCache categoryInfoCache;

    private OcrClient ocrClient;

    private FormalizerServiceStub formalizerService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        ocrClient = new OcrClient(
            ocrPath,
            apiKey,
            httpClient,
            objectMapper
        );

        formalizerService = new FormalizerServiceStub();
        formalizerService.setHost(formalizerHost);
        formalizerService.setUserAgent("test");

        categoryInfoCache = new CategoryInfoCache(categoryService);
    }

    @Test
    public void tryOcr() {
        try (FileOutputStream resultOut = new FileOutputStream(OUTPUT_FILE);
             PrintWriter resultPw = new PrintWriter(resultOut);
             FileOutputStream photoResultOut = new FileOutputStream(OUTPUT_PHOTOS);
             PrintWriter photoResultPw = new PrintWriter(photoResultOut);
             FileOutputStream paramsResultOut = new FileOutputStream(OUTPUT_PARAMS);
             PrintWriter paramsResultPw = new PrintWriter(paramsResultOut)) {
            Map<Long, String> categoryNames = categoryRepository.getFullCategoryTree().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
            Map<Long, Integer> totalCategoryParams = new HashMap<>();

            GoodFilter filter = new GoodFilter()
                .addState(GoodState.PHOTO_EDITED)
                .addState(GoodState.EDITING)
                .addState(GoodState.EDITED)
                .addState(GoodState.VERIFYING)
                .addState(GoodState.VERIFIED)
                .addState(GoodState.PREPARED_TO_OUT)
                .addState(GoodState.OUT);
            List<Good> goods = goodRepository.find(filter).stream()
                .filter(g -> g.getCleanupStatus() == CleanupStatus.NONE)
                .sorted(Comparator.comparing(g -> g.getCategoryId() == null ? 0 : g.getCategoryId()))
                .collect(Collectors.toList());

            log.info("Found {} goods with processed images", goods.size());

            goods.forEach(good -> {
                GoodResult goodResult = recognizeAndFormalize(good, categoryNames, totalCategoryParams);
                log.info(goodResult);
                resultPw.println(goodResult);
                goodResult.getPhotoResults().forEach(photoResultPw::println);
                goodResult.getParamResults().forEach(paramsResultPw::println);
            });
        } catch (Exception e) {
            log.error("Failed to work with output files", e);
        }
    }

    private GoodResult recognizeAndFormalize(Good good,
                                             Map<Long, String> categoryNames,
                                             Map<Long, Integer> totalCategoryParams) {
        List<EditedPhoto> editedPhotos = editedPhotoRepository.getProcessedPhotos(good.getId());
        List<TextPhotoResult> photoResults = editedPhotos.stream()
            .map(ep -> {
                TextPhotoResult result = new TextPhotoResult()
                    .setGood(good)
                    .setSuccess(false)
                    .setEditedPhoto(ep);
                try {
                    byte[] image = photoService
                        .readProcessedPhotoEdited(good.getWhBarcode(), good.getId(), ep.getPhoto());
                    recognizeAndFormalize(result, new ByteArrayInputStream(image));
                } catch (Exception e) {
                    log.error("Image read failed", e);
                    result.setErrorMessage("Image read failed: " + e.getMessage());
                }
                log.info(result.toString());
                return result;
            })
            .collect(Collectors.toList());

        return merge(good, categoryNames, totalCategoryParams, photoResults);
    }

    private GoodResult merge(Good good,
                             Map<Long, String> categoryNames,
                             Map<Long, Integer> totalCategoryParams,
                             List<TextPhotoResult> results) {
        String categoryName = categoryNames.get(good.getCategoryId());
        GoodResult result = new GoodResult()
            .setGood(good)
            .setCategoryName(categoryName)
            .setPhotoResults(results)
            .setTotalParamsCount(getOrReadTotalParamsCount(totalCategoryParams, good.getCategoryId()));
        Set<Integer> formalizedParamIds = new HashSet<>();
        Set<Pair<Integer, Object>> formalizedValues = new HashSet<>();
        for (TextPhotoResult textPhotoResult : results) {
            if (textPhotoResult.isSuccess()) {
                result.incSuccessCount();
            } else {
                result.incFailCount();
            }
            if (textPhotoResult.getParams() != null) {
                textPhotoResult.getParams().forEach(p -> {
                    formalizedParamIds.add(p.getParamId());
                    Object value = getValue(p);
                    formalizedValues.add(new Pair<>(p.getParamId(), value));
                });
            }
        }
        result.setParamsCount(formalizedParamIds.size());
        result.setParamValuesCount(formalizedValues.size());

        ModelEdit.Hierarchy hierarchy = GoodMskuHierarchy.getEditedHierarchy(good);
        if (hierarchy != null) {
            Set<Integer> hierarchyParamIds = new HashSet<>();
            Set<Pair<Integer, Object>> hierarchyValues = new HashSet<>();
            appendModelParameterValues(hierarchy.getModel(), hierarchyParamIds, hierarchyValues);
            appendModelParameterValues(hierarchy.getModification(), hierarchyParamIds, hierarchyValues);
            appendModelParameterValues(hierarchy.getSku(), hierarchyParamIds, hierarchyValues);
            result.setHierarchyParamsCount(hierarchyParamIds.size());
            result.setHierarchyValuesCount(hierarchyValues.size());
            formalizedValues.forEach(fv -> {
                if (!hierarchyParamIds.contains(fv.getFirst())) {
                    result.incNewValuesCount();
                    return;
                }
                if (hierarchyValues.contains(fv)) {
                    result.incValidValuesCount();
                } else {
                    result.incInvalidValuesCount();
                }
            });
            fillParamResults(result, good, categoryName, hierarchy);
        }
        return result;
    }

    public void fillParamResults(GoodResult goodResult,
                                 Good good,
                                 String categoryName,
                                 ModelEdit.Hierarchy hierarchy) {

        Map<Long, FormalizedParamValuesWithPictures> formalizedParamValues = new HashMap<>();

        for (TextPhotoResult photoResult : goodResult.getPhotoResults()) {
            if (CollectionUtils.isEmpty(photoResult.getParams()) ||
                photoResult.getEditedPhoto().getUploadedPicture() == null) {
                continue;
            }
            try {
                ModelStorage.Picture picture =
                    ModelStorage.Picture.parseFrom(photoResult.getEditedPhoto().getUploadedPicture());
                String url = picture.getUrlOrig();
                for (FormalizerParam.FormalizedParamPosition paramPosition : photoResult.getParams()) {
                    FormalizedParamValuesWithPictures paramValuesWithPictures = formalizedParamValues
                        .computeIfAbsent((long) paramPosition.getParamId(),
                            pp -> new FormalizedParamValuesWithPictures());

                    paramValuesWithPictures.addParamValue(paramPosition);
                    paramValuesWithPictures.addPictures(url);
                }
            } catch (InvalidProtocolBufferException e) {
                log.warn("Error parsing uploaded picture", e);
            }
        }

        if (formalizedParamValues.isEmpty()) {
            return;
        }

        Map<Long, List<ModelStorage.ParameterValue>> hierarchyParamValues = new HashMap<>();
        appendModelParameterValues(hierarchy.getModel(), hierarchyParamValues);
        appendModelParameterValues(hierarchy.getModification(), hierarchyParamValues);
        appendModelParameterValues(hierarchy.getSku(), hierarchyParamValues);

        formalizedParamValues.forEach((paramId, formalizedValues) -> {
            long categoryId = good.getCategoryId();
            CachedParamInfo paramInfo = categoryInfoCache.getParamInfo(categoryId, paramId);

            ParamResult paramResult = new ParamResult();
            paramResult.setCategoryId(categoryId);
            paramResult.setCategoryName(categoryName);
            paramResult.setModelId(hierarchy.getModel().getId());
            paramResult.setMskuId(good.getMskuId());
            paramResult.setBarcode(good.getWhBarcode());
            paramResult.setOfferTitle(good.getMskuTitle());
            paramResult.setParamId(paramId);
            paramResult.setParamName(paramInfo.getName());

            ModelStorage.ParameterValue urlValue = hierarchyParamValues
                .getOrDefault(7351726L, Collections.emptyList())
                .stream()
                .reduce((a, b) -> b).orElse(null);
            String url = "";
            if (urlValue != null) {
                url = urlValue.getStrValueList().stream()
                    .map(ModelStorage.LocalizedString::getValue)
                    .findFirst().orElse("");
            }
            paramResult.setUrl(url);

            List<ModelStorage.ParameterValue> modelValues =
                hierarchyParamValues.getOrDefault(paramId, Collections.emptyList());

            String formalizedValueText = formalizedValues.getParamValues().stream()
                .map(fv -> renderParameterValue(categoryId, fv))
                .sorted()
                .distinct()
                .collect(Collectors.joining(","));

            String modelValueText = modelValues.stream()
                .map(mv -> renderParameterValue(categoryId, mv))
                .sorted()
                .distinct()
                .collect(Collectors.joining(","));

            List<String> pictureUrls = formalizedValues.getPictures().stream()
                .map(pictureUrl -> "http:" + pictureUrl)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

            paramResult.setValue(formalizedValueText);
            paramResult.setModelValue(modelValueText);
            paramResult.setPictureUrls(pictureUrls);

            String resultText = "invalid";
            if (StringUtils.isEmpty(modelValueText)) {
                resultText = "new";
            } else if (modelValueText.equals(formalizedValueText)) {
                resultText = "valid";
            }
            paramResult.setResult(resultText);

            goodResult.addParamResult(paramResult);
        });
    }

    public String renderParameterValue(long categoryId, FormalizerParam.FormalizedParamPosition parameterValue) {
        switch (parameterValue.getType()) {
            case BOOLEAN:
                return parameterValue.getBooleanValue() ? "да" : "нет";
            case ENUM:
            case NUMERIC_ENUM:
                return categoryInfoCache.getValueInfo(categoryId, (long) parameterValue.getOptionId()).getName();
            case NUMERIC:
                return String.valueOf(parameterValue.getNumberValue());
            default:
                throw new IllegalArgumentException("unexpected parameter type " + parameterValue.getType());
        }
    }


    public String renderParameterValue(long categoryId, ModelStorage.ParameterValue parameterValue) {
        switch (parameterValue.getValueType()) {
            case BOOLEAN:
                return parameterValue.getBoolValue() ? "да" : "нет";
            case STRING:
                return parameterValue.getStrValueList().stream()
                    .map(ModelStorage.LocalizedString::getValue)
                    .collect(Collectors.joining(", "));
            case ENUM:
            case NUMERIC_ENUM:
                return categoryInfoCache.getValueInfo(categoryId, (long) parameterValue.getOptionId()).getName();
            case NUMERIC:
                return parameterValue.getNumericValue();
            default:
                throw new IllegalArgumentException("unexpected parameter type " + parameterValue.getValueType());
        }
    }

    private void appendModelParameterValues(ModelStorage.Model model,
                                            Set<Integer> hierarchyParamIds,
                                            Set<Pair<Integer, Object>> hierarchyValues) {
        if (model == null) {
            return;
        }
        model.getParameterValuesList().forEach(p -> {
            hierarchyParamIds.add((int) p.getParamId());
            Object value = getValue(p);
            if (value != null) {
                hierarchyValues.add(new Pair<>((int) p.getParamId(), value));
            }
        });
    }

    private void appendModelParameterValues(ModelStorage.Model model,
                                            Map<Long, List<ModelStorage.ParameterValue>> hierarchyValues) {
        if (model == null) {
            return;
        }
        model.getParameterValuesList().forEach(p -> {
            hierarchyValues.computeIfAbsent(p.getParamId(), (k) -> new ArrayList<>())
                .add(p);
        });
    }

    private Integer getOrReadTotalParamsCount(Map<Long, Integer> totalCategoryParams, long categoryId) {
        return totalCategoryParams.computeIfAbsent(categoryId, (hid) -> {
            Category category = categoryRepository.getByHid(hid);
            MboParameters.Category categoryDef = CategoryUtils.getCategoryDescription(category);
            String form = categoryDef.getContentLabForm();
            if (form == null) {
                return 0;
            }
            try {
                EditorForm editorForm = objectMapper.readValue(form, EditorForm.class);
                return (int) editorForm.getTabs().stream()
                    .map(EditorTab::getBlocks)
                    .flatMap(Collection::stream)
                    .map(EditorBlock::getProperties)
                    .flatMap(Collection::stream)
                    .count();
            } catch (IOException e) {
                log.error("Failed to parse form for category " + categoryId, e);
                return 0;
            }
        });
    }

    private Object getValue(FormalizerParam.FormalizedParamPosition paramPosition) {
        switch (paramPosition.getType()) {
            case ENUM:
            case NUMERIC_ENUM:
                return paramPosition.getOptionId();
            case BOOLEAN:
                return paramPosition.getBooleanValue();
            case NUMERIC:
                return paramPosition.getNumberValue();
            default:
                throw new IllegalArgumentException();
        }
    }

    private Object getValue(ModelStorage.ParameterValue parameterValue) {
        switch (parameterValue.getValueType()) {
            case ENUM:
            case NUMERIC_ENUM:
                return parameterValue.getOptionId();
            case BOOLEAN:
                return parameterValue.getBoolValue();
            case NUMERIC:
                return Double.valueOf(parameterValue.getNumericValue());
            case STRING:
                return parameterValue.getStrValueList();
            default:
                throw new IllegalArgumentException();
        }
    }


    private void recognizeAndFormalize(TextPhotoResult result,
                                       InputStream stream) {
        try {
            RecognizeResponse response = ocrClient.recognize(stream);
            if (!StringUtils.isEmpty(response.getErrorMessage())) {
                result.setErrorMessage(response.getErrorMessage());
            } else if (response.getData() == null) {
                result.setErrorMessage("No data field in response");
            } else if (CollectionUtils.isEmpty(response.getData().getFulltext())) {
                result.setErrorMessage("No fulltext field in response");
            } else {
                String text = response.getData().getFulltext().stream()
                    .map(FullText::getText)
                    .collect(Collectors.joining());
                result.setText(text);
                formalize(result);
            }
        } catch (Exception e) {
            String message = "OCR request failed: " + e.getMessage();
            log.error(message, e);
            result.setErrorMessage(message);
        }
    }

    private void formalize(TextPhotoResult result) {
        try {
            Formalizer.Offer.Builder offer = Formalizer.Offer.newBuilder()
                .setApplyChangeCategoryRules(false)
                .setCategoryId(result.getGood().getCategoryId().intValue())
                .setTitle("")
                .setDescription(result.getText())
                .setRedStatus(1);

            Formalizer.FormalizerRequest request = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnParamXslName(true)
                .setReturnValueName(true)
                .build();

            String humanReadableRequest = JsonFormat.printToString(request);
            log.info(humanReadableRequest);

            Formalizer.FormalizerResponse formalizerResponse = formalizerService.formalize(request);
            Formalizer.FormalizedOffer formalizedOffer = formalizerResponse.getOffer(0);

            result.setParams(formalizedOffer.getPositionList());
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("Formalization failed", e);
            result.setErrorMessage("Formalization failed: " + e.getMessage());
        }
    }

    class GoodResult {
        private int successCount;
        private int failCount;
        private int paramValuesCount;
        private int validValuesCount;
        private int newValuesCount;
        private int invalidValuesCount;
        private int hierarchyValuesCount;
        private int paramsCount;
        private int hierarchyParamsCount;
        private int totalParamsCount;
        private Good good;
        private String categoryName;
        private List<TextPhotoResult> photoResults;
        private List<ParamResult> paramResults = new ArrayList<>();

        public int isSuccessCount() {
            return successCount;
        }

        public GoodResult incSuccessCount() {
            this.successCount++;
            return this;
        }

        public int isFailCount() {
            return failCount;
        }

        public GoodResult incFailCount() {
            this.failCount++;
            return this;
        }

        public int getParamValuesCount() {
            return paramValuesCount;
        }

        public GoodResult setParamValuesCount(int paramValuesCount) {
            this.paramValuesCount = paramValuesCount;
            return this;
        }


        public int getValidValuesCount() {
            return validValuesCount;
        }

        public GoodResult incValidValuesCount() {
            this.validValuesCount++;
            return this;
        }

        public int getNewValuesCount() {
            return newValuesCount;
        }

        public GoodResult incNewValuesCount() {
            this.newValuesCount++;
            return this;
        }

        public int getInvalidValuesCount() {
            return invalidValuesCount;
        }

        public GoodResult incInvalidValuesCount() {
            this.invalidValuesCount++;
            return this;
        }

        public int getParamsCount() {
            return paramsCount;
        }

        public GoodResult setParamsCount(int paramsCount) {
            this.paramsCount = paramsCount;
            return this;
        }

        public int getTotalParamsCount() {
            return totalParamsCount;
        }

        public GoodResult setTotalParamsCount(int totalParamsCount) {
            this.totalParamsCount = totalParamsCount;
            return this;
        }

        public int getHierarchyValuesCount() {
            return hierarchyValuesCount;
        }

        public GoodResult setHierarchyValuesCount(int hierarchyValuesCount) {
            this.hierarchyValuesCount = hierarchyValuesCount;
            return this;
        }

        public int getHierarchyParamsCount() {
            return hierarchyParamsCount;
        }

        public GoodResult setHierarchyParamsCount(int hierarchyParamsCount) {
            this.hierarchyParamsCount = hierarchyParamsCount;
            return this;
        }

        public Good getGood() {
            return good;
        }

        public GoodResult setGood(Good good) {
            this.good = good;
            return this;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public GoodResult setCategoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public List<TextPhotoResult> getPhotoResults() {
            return photoResults;
        }

        public GoodResult setPhotoResults(List<TextPhotoResult> photoResults) {
            this.photoResults = photoResults;
            return this;
        }

        public List<ParamResult> getParamResults() {
            return paramResults;
        }

        public void addParamResult(ParamResult paramResult) {
            this.paramResults.add(paramResult);
        }

        @Override
        public String toString() {
            return "GoodResult{" +
                "successCount=" + successCount +
                ", failCount=" + failCount +
                ", paramValuesCount=" + paramValuesCount +
                ", newValuesCount=" + newValuesCount +
                ", validValuesCount=" + validValuesCount +
                ", invalidValuesCount=" + invalidValuesCount +
                ", hierarchyValuesCount=" + hierarchyValuesCount +
                ", paramsCount=" + paramsCount +
                ", hierarchyParamsCount=" + hierarchyParamsCount +
                ", totalParamsCount=" + totalParamsCount +
                ", goodId=" + good.getId() +
                ", goodBarcode=" + good.getWhBarcode() +
                ", goodMskuId=" + good.getMskuId() +
                ", goodMskuTitle=" + good.getMskuTitle() +
                ", goodCategoryId=" + good.getCategoryId() +
                ", categoryName=" + categoryName +
                '}';
        }
    }

    class TextPhotoResult {
        private boolean success;
        private String errorMessage;
        private String text;
        private Good good;
        private EditedPhoto editedPhoto;
        private List<FormalizerParam.FormalizedParamPosition> params;

        public boolean isSuccess() {
            return success;
        }

        public TextPhotoResult setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public String isErrorMessage() {
            return errorMessage;
        }

        public TextPhotoResult setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public String getText() {
            return text;
        }

        public TextPhotoResult setText(String text) {
            this.text = text;
            return this;
        }

        public Good getGood() {
            return good;
        }

        public TextPhotoResult setGood(Good good) {
            this.good = good;
            return this;
        }

        public EditedPhoto getEditedPhoto() {
            return editedPhoto;
        }

        public TextPhotoResult setEditedPhoto(EditedPhoto editedPhoto) {
            this.editedPhoto = editedPhoto;
            return this;
        }

        public List<FormalizerParam.FormalizedParamPosition> getParams() {
            return params;
        }

        public TextPhotoResult setParams(List<FormalizerParam.FormalizedParamPosition> params) {
            this.params = params;
            return this;
        }

        @Override
        public String toString() {
            return "TextPhotoResult{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", textLength='" + (text == null ? 0 : text.length()) + '\'' +
                ", goodId=" + good.getId() +
                ", goodBarcode=" + good.getWhBarcode() +
                ", editedPhotoId=" + editedPhoto.getId() +
                ", editedPhoto=" + editedPhoto.getPhoto() +
                ", paramValues=" + (params == null ? 0 : params.size()) +
                '}';
        }
    }

    class ParamResult {
        long categoryId;
        String categoryName;
        long modelId;
        long mskuId;
        String barcode;
        String url;
        String offerTitle;
        List<String> pictureUrls;
        long paramId;
        String paramName;
        String value;
        String modelValue;
        String result;

        public long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public long getModelId() {
            return modelId;
        }

        public void setModelId(long modelId) {
            this.modelId = modelId;
        }

        public long getMskuId() {
            return mskuId;
        }

        public void setMskuId(long mskuId) {
            this.mskuId = mskuId;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getOfferTitle() {
            return offerTitle;
        }

        public void setOfferTitle(String offerTitle) {
            this.offerTitle = offerTitle;
        }

        public long getParamId() {
            return paramId;
        }

        public void setParamId(long paramId) {
            this.paramId = paramId;
        }

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public List<String> getPictureUrls() {
            return pictureUrls;
        }

        public void setPictureUrls(List<String> pictureUrls) {
            this.pictureUrls = pictureUrls;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getModelValue() {
            return modelValue;
        }

        public void setModelValue(String modelValue) {
            this.modelValue = modelValue;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return categoryId +
                "\t" + categoryName +
                "\t" + modelId +
                "\t" + mskuId +
                "\t" + barcode +
                "\t" + url +
                "\t" + offerTitle +
                "\t" + paramId +
                "\t" + paramName +
                "\t" + value +
                "\t" + modelValue +
                "\t" + result +
                "\t" + String.join("\t", pictureUrls);
        }
    }

    class FormalizedParamValuesWithPictures {
        List<FormalizerParam.FormalizedParamPosition> paramValues = new ArrayList<>();
        Set<String> pictures = new HashSet<>();

        public List<FormalizerParam.FormalizedParamPosition> getParamValues() {
            return paramValues;
        }

        public void addParamValue(FormalizerParam.FormalizedParamPosition paramValue) {
            this.paramValues.add(paramValue);
        }

        public Set<String> getPictures() {
            return pictures;
        }

        public void addPictures(String picture) {
            this.pictures.add(picture);
        }
    }
}
