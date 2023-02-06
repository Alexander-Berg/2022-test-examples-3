package ru.yandex.direct.core.entity.outdoor.repository;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.outdoor.model.OutdoorOperator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.dbschema.ppcdict.tables.OutdoorOperators.OUTDOOR_OPERATORS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OutdoorOperatorRepositoryTest {

    private static final String LOGIN = "testoperator";
    @Autowired
    private OutdoorOperatorRepository outdoorOperatorRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void clear() {
        dslContextProvider.ppcdict()
                .deleteFrom(OUTDOOR_OPERATORS).execute();
    }

    @Test
    public void addOrUpdateOperators_AddOperator_Success() {
        OutdoorOperator operator = new OutdoorOperator().withLogin(LOGIN).withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));
        OutdoorOperator actualOperator = outdoorOperatorRepository.getByLogin(LOGIN);
        assertThat(actualOperator).isEqualTo(operator);
    }

    @Test
    public void addOrUpdateOperators_AddAndUpdateOperator_Success() {
        OutdoorOperator operator = new OutdoorOperator().withLogin(LOGIN).withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        operator.setName("another name");
        OutdoorOperator newOperator = new OutdoorOperator()
                .withLogin("outdoors")
                .withName("Outdoors");
        outdoorOperatorRepository.addOrUpdateOperators(asList(operator, newOperator));

        List<OutdoorOperator> allOperators = outdoorOperatorRepository.getAllOperators(LimitOffset.maxLimited());
        assertThat(allOperators).containsExactlyInAnyOrder(operator, newOperator);
    }

    @Test
    public void addOrUpdateOperators_UpdateOperator_Success() {
        OutdoorOperator operator = new OutdoorOperator().withLogin(LOGIN).withName("name_of_operator");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        operator.setName("another name");
        outdoorOperatorRepository.addOrUpdateOperators(singletonList(operator));

        OutdoorOperator actualOperator = outdoorOperatorRepository.getByLogin(LOGIN);
        assertThat(actualOperator).isEqualTo(operator);
    }

    @Test
    public void getAllOperators_Success() {
        OutdoorOperator operator = new OutdoorOperator().withLogin(LOGIN).withName("name_of_operator");
        OutdoorOperator operator2 = new OutdoorOperator().withLogin("testoperator2").withName("name_of_operator2");
        outdoorOperatorRepository.addOrUpdateOperators(asList(operator, operator2));
        List<OutdoorOperator> operators = outdoorOperatorRepository.getAllOperators(new LimitOffset(1, 0));
        Assert.assertThat(operators, hasSize(1));

        List<OutdoorOperator> operatorsBig = outdoorOperatorRepository.getAllOperators(LimitOffset.limited(100));
        Assert.assertThat(operatorsBig, hasSize(2));
    }
}
