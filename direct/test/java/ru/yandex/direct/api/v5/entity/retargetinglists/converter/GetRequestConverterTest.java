package ru.yandex.direct.api.v5.entity.retargetinglists.converter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.retargetinglists.GetRequest;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListSelectionCriteria;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListTypeEnum;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelectionCriteria;
import ru.yandex.direct.dbschema.ppc.enums.RetargetingConditionsRetargetingConditionsType;

import static com.yandex.direct.api.v5.retargetinglists.RetargetingListTypeEnum.AUDIENCE;
import static com.yandex.direct.api.v5.retargetinglists.RetargetingListTypeEnum.RETARGETING;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.dbschema.ppc.enums.RetargetingConditionsRetargetingConditionsType.interests;
import static ru.yandex.direct.dbschema.ppc.enums.RetargetingConditionsRetargetingConditionsType.metrika_goals;

@Api5Test
@RunWith(Parameterized.class)
public class GetRequestConverterTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private GetRequestConverter getRequestConverter;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public List<Long> requestIds;

    @Parameterized.Parameter(2)
    public List<RetargetingListTypeEnum> requestTypes;

    @Parameterized.Parameter(3)
    public Set<Long> expectedIds;

    @Parameterized.Parameter(4)
    public Set<RetargetingConditionsRetargetingConditionsType> expectedTypes;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"ids - null, types - null",
                        null, null,
                        null, asSet(metrika_goals, interests)},
                {"ids - empty, types - null",
                        emptyList(), null,
                        null, asSet(metrika_goals, interests)},
                {"ids - null, types - empty",
                        null, emptyList(),
                        null, asSet(metrika_goals, interests)},
                {"ids - null, types - retargeting",
                        null, singletonList(RETARGETING),
                        null, singleton(metrika_goals)},
                {"ids - null, types - audience",
                        null, singletonList(AUDIENCE),
                        null, singleton(interests)},
                {"ids - full, types - full",
                        asList(123L, 456L), asList(RETARGETING, AUDIENCE),
                        asSet(123L, 456L), asSet(metrika_goals, interests)},
        };
        return asList(data);
    }

    @Test
    public void convert() {
        GetRequest getRequest = new GetRequest()
                .withSelectionCriteria(new RetargetingListSelectionCriteria()
                        .withIds(requestIds)
                        .withTypes(requestTypes));

        RetargetingSelectionCriteria actualCriteria = getRequestConverter.convert(getRequest);

        RetargetingSelectionCriteria expectedCriteria = new RetargetingSelectionCriteria()
                .withIds(expectedIds)
                .withTypes(expectedTypes);

        assertThat(actualCriteria)
                .usingRecursiveComparison()
                .isEqualTo(expectedCriteria);
    }

}
