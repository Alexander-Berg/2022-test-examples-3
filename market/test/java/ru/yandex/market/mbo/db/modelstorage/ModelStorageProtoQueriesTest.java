package ru.yandex.market.mbo.db.modelstorage;

import java.util.List;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.index.Operation;
import ru.yandex.market.mbo.db.modelstorage.index.saas.SaasIndexReader;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelQuality;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.CREATED_DATE;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.DELETED_DATE;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.MODIFIED_DATE;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.TITLE;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.EQ;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.GTE;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.LT;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 19.12.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelStorageProtoQueriesTest {

    private ModelStorageProtoService protoService;

    @Rule
    public ExpectedException failure = ExpectedException.none();

    @Before
    public void before() {
        protoService = new ModelStorageProtoService();
    }

    @Test
    public void simpleModelNameMboFilter() {
        ModelStorageProtoService.MboFilters queries = protoService.makeFilter(modelNameRequest("simple"));
        assertThat(extractValue(queries.getSingleFilter(), TITLE, EQ), is("simple"));
    }

    @Test
    public void simpleModelNameTrimMboFilter() {
        ModelStorageProtoService.MboFilters queries = protoService.makeFilter(modelNameRequest("  model  name  "));
        assertThat(extractValue(queries.getSingleFilter(), TITLE, EQ), is("model  name"));
    }

    @Test
    public void splitQueryByChunks() {
        ModelStorage.FindModelsRequest.Builder request = ModelStorage.FindModelsRequest.newBuilder();
        IntStream.range(1, (int) (SaasIndexReader.MAX_SAAS_IN_CLAUSE * 1.4)).forEach(request::addModelIds);

        ModelStorageProtoService.MboFilters queries = protoService.makeFilter(request.build());
        assertThat(queries.hasModelIds(), is(true));
        assertThat(queries.getFilters(), hasSize(2));
    }

    @Test
    public void usePageSearchIfOffset() {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setOffset(10)
            .build();

        assertThat(protoService.makeFilter(request).usePageSearch(), is(true));
    }

    @Test
    public void usePageSearchIfSort() {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.MODIFIED_DATE)
            .build();

        assertThat(protoService.makeFilter(request).usePageSearch(), is(true));
    }

    @Test
    public void errorIfTooManyModelIds() {
        failure.expect(IllegalArgumentException.class);
        failure.expectMessage("Maximum allowed rows is");

        ModelStorage.FindModelsRequest.Builder request = ModelStorage.FindModelsRequest.newBuilder();
        IntStream.range(1, 1005).forEach(request::addModelIds);
        protoService.makeFilter(request.build());
    }

    @Test
    public void errorIfPageAndCursorMark() {
        failure.expect(IllegalArgumentException.class);
        failure.expectMessage(is("Using cursor mark with offset or sort is unsupported"));

        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.MODIFIED_DATE)
            .setCursorMark("123456")
            .build();

        protoService.makeFilter(request);
    }

    @Test
    public void hasModelIdsFalse() {
        assertThat(protoService.makeFilter(modelNameRequest("test")).hasModelIds(), is(false));
    }

    @Test
    public void usePageFalseByDefault() {
        assertThat(protoService.makeFilter(modelNameRequest("test")).usePageSearch(), is(false));
    }

    @Test
    public void useOnlyOperatorQualityCriteria() {
        ModelStorage.FindModelsRequest.Builder request = ModelStorage.FindModelsRequest.newBuilder()
            .setOnlyOperatorQuality(true);

        ModelStorageProtoService.MboFilters filters = protoService.makeFilter(request.build());

        List<ParamValueSearch> attributes = filters.getSingleFilter().getAttributes();
        assertThat(attributes.size(), is(1));
        ParameterValues values = attributes.get(0).getParameterValues();
        assertThat(values.getParamId(), is(KnownIds.MODEL_QUALITY_PARAM_ID));
        assertThat(
            values.getOptionIds(),
            containsInAnyOrder(
                ModelQuality.OFFER.getOptionId(),
                ModelQuality.DUMMY.getOptionId(),
                ModelQuality.PARTNER.getOptionId()
            )
        );
    }

    @Test
    public void findWithModification() {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .addModelIds(2L)
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.MODIFIED_DATE)
            .setWithModification(true)
            .build();

        ModelStorageProtoService.MboFilters queries = protoService.makeFilter(request);
        assertThat(queries.hasModelIds(), is(true));
        assertThat(queries.getFilters(), hasSize(2));
        assertThat(queries.getFilters().get(1).getParentIds(), contains(2L));
    }

    @NotNull
    private ModelStorage.FindModelsRequest modelNameRequest(String s) {
        return ModelStorage.FindModelsRequest.newBuilder()
            .setModelName(s)
            .build();
    }

    @Test
    public void findByDates() {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setStartCreatedDate(1L)
            .setFinishCreatedDate(2L)
            .setStartDeletedDate(3L)
            .setFinishDeletedDate(4L)
            .setStartModifiedDate(5L)
            .setFinishModifiedDate(6L)
            .build();

        ModelStorageProtoService.MboFilters queries = protoService.makeFilter(request);
        MboIndexesFilter filter = queries.getSingleFilter();

        assertThat(extractValue(filter, CREATED_DATE, GTE), is(1L));
        assertThat(extractValue(filter, CREATED_DATE, LT), is(2L));

        assertThat(extractValue(filter, DELETED_DATE, GTE), is(3L));
        assertThat(extractValue(filter, DELETED_DATE, LT), is(4L));

        assertThat(extractValue(filter, MODIFIED_DATE, GTE), is(5L));
        assertThat(extractValue(filter, MODIFIED_DATE, LT), is(6L));
    }

    private Object extractValue(MboIndexesFilter filter, GenericField field, Operation operation) {
        return filter.getOperation(field, operation).get().getValue();
    }
}
