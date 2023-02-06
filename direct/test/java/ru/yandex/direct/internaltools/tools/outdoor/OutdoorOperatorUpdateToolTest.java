package ru.yandex.direct.internaltools.tools.outdoor;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.outdoor.model.OutdoorOperator;
import ru.yandex.direct.core.entity.outdoor.repository.OutdoorOperatorRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.tools.outdoor.model.OutdoorOperatorUpdateParameter;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.OutdoorOperators.OUTDOOR_OPERATORS;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class OutdoorOperatorUpdateToolTest {

    @Autowired
    private OutdoorOperatorRepository outdoorOperatorRepository;
    @Autowired
    private OutdoorOperatorUpdateTool outdoorOperatorUpdateTool;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void clear() {
        dslContextProvider.ppcdict()
                .deleteFrom(OUTDOOR_OPERATORS).execute();
    }

    @Test
    public void successfullyAddOperator() {
        OutdoorOperatorUpdateParameter params = new OutdoorOperatorUpdateParameter()
                .withLogin("gallery.client")
                .withOperatorName("Gallery Outdoor");
        InternalToolMassResult<OutdoorOperator> processResult = outdoorOperatorUpdateTool.process(params);
        assertThat(processResult.getData(), contains(new OutdoorOperator()
                .withLogin(params.getLogin())
                .withName(params.getOperatorName())));
    }

    @Test
    public void successfullyUpdateOperator() {
        OutdoorOperator operator = new OutdoorOperator()
                .withLogin("test login")
                .withName("Тестовый оператор");
        outdoorOperatorRepository.addOrUpdateOperators(Collections.singletonList(operator));

        OutdoorOperatorUpdateParameter params = new OutdoorOperatorUpdateParameter()
                .withLogin(operator.getLogin())
                .withOperatorName("Outdoor Тестовый оператор");
        InternalToolMassResult<OutdoorOperator> processResult = outdoorOperatorUpdateTool.process(params);
        assertThat(processResult.getData(), contains(operator.withName(params.getOperatorName())));
    }
}
