package ru.yandex.market.mbo.validator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceMock;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author dmserebr
 * @date 22/02/2019
 */
public class OptionPropertyDuplicationValidatorTest {

    protected static final long PARAM_1_OPTION_1 = 101L;
    protected static final long PARAM_1_OPTION_2 = 102L;
    protected static final long PARAM_1_OPTION_3 = 103L;
    protected static final long PARAM_2_OPTION_1 = 201L;
    protected static final long PARAM_2_OPTION_2 = 202L;

    protected ValueLinkServiceMock valueLinkService;

    protected ParameterLoaderServiceStub parameterLoaderService;
    protected Parameter param1;
    protected Parameter param2;
    protected Multimap<Long, Long> optionIdsByParamId = ArrayListMultimap.create();

    public void before() {
        parameterLoaderService = new ParameterLoaderServiceStub();
        valueLinkService = new ValueLinkServiceMock();
        valueLinkService.setOptionIdsByParamId(optionIdsByParamId);

        param1 = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second"))
            .build();
        param2 = CategoryParamBuilder.newBuilder(2, "param-2", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_2_OPTION_1).addName("param-2-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_2_OPTION_2).addName("param-2-second"))
            .build();

        parameterLoaderService.addAllCategoryParams(Arrays.asList(param1, param2));
    }

    @SafeVarargs
    static <T> Set<T> set(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
