package ru.yandex.market.mbo.db.params;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ParameterChanges.ParameterChangeResponse;
import ru.yandex.market.mbo.http.ParameterChanges.ParameterOptionChange;
import ru.yandex.market.mbo.http.ParameterChanges.ParameterOptionsRequest;
import ru.yandex.market.mbo.http.ParameterChangesService;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.http.ParameterChanges.ParameterAliasChange;
import static ru.yandex.market.mbo.http.ParameterChanges.ParameterAliasesRequest;
import static ru.yandex.market.mbo.http.ParameterChanges.ParameterChangesResponse;

/**
 * @author galaev@yandex-team.ru
 * @since 18/04/2017.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ParameterChangesServiceTest {

    private static final int CATEGORY_ID = 1;
    private ParameterChangesService parameterChangesService;
    private Map<Long, CategoryParam> categoryParameters;

    @Before
    public void setUp() throws Exception {
        fillParameters();
        IParameterLoaderService parameterLoaderService = new ParameterLoaderService(
            null, null, null, null, null, null, -1) {
            @Override
            public CategoryEntities loadCategoryEntitiesByHid(long hid) {
                CategoryEntities entities = new CategoryEntities();
                entities.setParameters(new ArrayList<>(categoryParameters.values()));
                return entities;
            }
        };
        ParameterService parameterService = Mockito.mock(ParameterService.class);
        Mockito.doCallRealMethod().when(parameterService).createDefaultSaveContext(Mockito.anyLong());
        Mockito.doAnswer(invok -> {
            CategoryParam p = invok.getArgument(2);
            ParameterValuesChanges valuesChanges = invok.getArgument(3);
            valuesChanges.getAdded().forEach(p::addOption);
            valuesChanges.getUpdated().forEach(o -> {
                p.removeOption(o);
                p.addOption(o);
            });
            return null;
        })
            .when(parameterService)
            .saveParameter(
                Mockito.any(ParameterSaveContext.class),
                Mockito.anyLong(),
                Mockito.any(CategoryParam.class),
                Mockito.any(ParameterValuesChanges.class)
            );
        AutoUser autoUser = new AutoUser(0);

        parameterChangesService = new ParameterChangesServiceImpl();
        ReflectionTestUtils.setField(parameterChangesService, "parameterLoader", parameterLoaderService);
        ReflectionTestUtils.setField(parameterChangesService, "parameterService", parameterService);
        ReflectionTestUtils.setField(parameterChangesService, "autoUser", autoUser);
    }

    private void fillParameters() {
        Parameter p1 = new Parameter();
        p1.setId(1);
        p1.addName(WordUtil.defaultWord("name1"));
        Parameter p2 = new Parameter();
        p2.setId(2);
        p2.addName(WordUtil.defaultWord("name2"));
        p2.addOption(new OptionImpl(1, "optionName"));
        categoryParameters = new HashMap<>();
        categoryParameters.put(p1.getId(), p1);
        categoryParameters.put(p2.getId(), p2);
    }

    @Test
    public void addParameterAlias() throws Exception {
        String alias = "alias1";
        long parameterId = 1;
        int reqId = 1;

        ParameterAliasesRequest request = ParameterAliasesRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addParameterAliases(ParameterAliasChange.newBuilder()
                        .setReqId(reqId)
                        .setParameterId(parameterId)
                        .setParamAlias(alias))
                .build();
        ParameterChangesResponse response = parameterChangesService.addParameterAliases(request);
        assertEquals(1, response.getResponsesCount());

        ParameterChangeResponse changeResponse = response.getResponsesList().get(0);
        assertEquals(reqId, changeResponse.getReqId());
        assertTrue(changeResponse.getStatus());

        CategoryParam changedParameter = categoryParameters.get(parameterId);
        assertTrue(changedParameter.getDefaultAliases().contains(alias));
    }

    @Test
    public void addDuplicateParameterAlias() throws Exception {
        String alias = "name2";
        long parameterId = 2;
        int reqId = 2;

        ParameterAliasesRequest request = ParameterAliasesRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addParameterAliases(ParameterAliasChange.newBuilder()
                        .setReqId(reqId)
                        .setParameterId(parameterId)
                        .setParamAlias(alias))
                .build();
        ParameterChangesResponse response = parameterChangesService.addParameterAliases(request);
        assertEquals(1, response.getResponsesCount());

        ParameterChangeResponse changeResponse = response.getResponsesList().get(0);
        assertEquals(reqId, changeResponse.getReqId());
        assertFalse(changeResponse.getStatus());

        CategoryParam changedParameter = categoryParameters.get(parameterId);
        assertFalse(changedParameter.getDefaultAliases().contains(alias));
    }

    @Test
    public void addOption() throws Exception {
        String option = "option1";
        long parameterId = 1;
        long optionId = 0;
        int reqId = 1;

        ParameterOptionsRequest request = ParameterOptionsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addParameterOptions(ParameterOptionChange.newBuilder()
                        .setReqId(reqId)
                        .setParameterId(parameterId)
                        .setOptionId(optionId)
                        .setNewOption(option))
                .build();
        ParameterChangesResponse response = parameterChangesService.addParameterOptions(request);
        assertEquals(1, response.getResponsesCount());

        ParameterChangeResponse changeResponse = response.getResponsesList().get(0);
        assertEquals(reqId, changeResponse.getReqId());
        assertTrue(changeResponse.getStatus());

        CategoryParam changedParameter = categoryParameters.get(parameterId);
        Assertions.assertThat(changedParameter.getOptions())
            .extracting(Option::getName)
            .containsExactlyInAnyOrder(option);
    }

    @Test
    public void addOptionAlias() throws Exception {
        String optionAlias = "optionAlias";
        long parameterId = 2;
        long optionId = 1;
        int reqId = 1;

        ParameterOptionsRequest request = ParameterOptionsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addParameterOptions(ParameterOptionChange.newBuilder()
                        .setReqId(reqId)
                        .setParameterId(parameterId)
                        .setOptionId(optionId)
                        .setOptionAlias(optionAlias))
                .build();
        ParameterChangesResponse response = parameterChangesService.addParameterOptions(request);
        assertEquals(1, response.getResponsesCount());

        ParameterChangeResponse changeResponse = response.getResponsesList().get(0);
        assertEquals(reqId, changeResponse.getReqId());
        assertTrue(changeResponse.getStatus());

        CategoryParam changedParameter = categoryParameters.get(parameterId);
        assertTrue(changedParameter.getOptions().stream()
                .filter(o -> o.getId() == optionId)
                .findFirst().get()
                .getAliases(EnumAlias.Type.GENERAL)
                .contains(optionAlias));
    }
}
