package ru.yandex.market.crm.campaign.services.actions.steps;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.services.actions.contexts.CoinIssuanceStepContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.ActionExecutionContext;
import ru.yandex.market.crm.campaign.yql.ExecuteYqlTaskData;
import ru.yandex.market.crm.campaign.yql.ResourceWithParameters;
import ru.yandex.market.crm.yql.YqlTemplateService;
import ru.yandex.market.crm.yql.client.YqlClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author zloddey
 */
public class PrepareToCoinIssueTableStepTest {

    public static final YPath STEP_INPUT_TABLE = YPath.simple("//previous_step/output");
    public static final YPath NO_AUTH_COUNT_TABLE = YPath.simple("//this_step/no_auth_count");
    public static final YPath AUTH_BUNCH_INPUT_TABLE = YPath.simple("//auth/bunch/input");

    private static class FakeCoinIssuanceStepContext extends CoinIssuanceStepContext {
        private final Integer actionSize;

        public FakeCoinIssuanceStepContext() {
            this(null);
        }

        public FakeCoinIssuanceStepContext(Integer actionSize) {
            super(mock(ActionExecutionContext.class), null);
            this.actionSize = actionSize;
        }

        @Override
        public IssueCoinsStep getStep() {
            IssueCoinsStep step = new IssueCoinsStep();
            step.setSizeLimit(actionSize);
            return step;
        }

        @Nonnull
        @Override
        public YPath getInputTable() {
            return STEP_INPUT_TABLE;
        }

        @Override
        public YPath getAuthBunchInputPath() {
            return AUTH_BUNCH_INPUT_TABLE;
        }

        @Override
        public YPath getNoAuthCounterTable() {
            return NO_AUTH_COUNT_TABLE;
        }
    }

    /**
     * Параметры, которые передаются в шаблон YQL при отсутствии лимита на размер акции
     */
    @Test
    public void noLimit() {
        YqlClient yqlClient = null;
        YqlTemplateService yqlTemplateService = null;
        CoinIssuanceStepContext context = new FakeCoinIssuanceStepContext();
        ExecuteYqlTaskData data = null;

        var step = new PrepareToCoinIssueTableStep(yqlClient, yqlTemplateService);
        Optional<ResourceWithParameters> optionalTemplate = step.generateCommand(context, data);
        assertTrue(optionalTemplate.isPresent());

        Map<String, Object> parameters = optionalTemplate.get().getParameters().build();
        assertEquals(6, parameters.size());

        assertEquals(STEP_INPUT_TABLE, parameters.get("input"));
        assertEquals(AUTH_BUNCH_INPUT_TABLE, parameters.get("outputPuid"));
        assertEquals(NO_AUTH_COUNT_TABLE, parameters.get("outputYuidCount"));
        assertNull(parameters.get("sizeLimit"));
        assertNull(parameters.get("vars"));
        assertEquals(Map.of(), parameters.get("enums"));
    }

    /**
     * Параметры, которые передаются в шаблон YQL при наличии лимита на размер акции
     */
    @Test
    public void withLimit() {
        YqlClient yqlClient = null;
        YqlTemplateService yqlTemplateService = null;
        CoinIssuanceStepContext context = new FakeCoinIssuanceStepContext(33);
        ExecuteYqlTaskData data = null;

        var step = new PrepareToCoinIssueTableStep(yqlClient, yqlTemplateService);
        Optional<ResourceWithParameters> optionalTemplate = step.generateCommand(context, data);
        assertTrue(optionalTemplate.isPresent());

        Map<String, Object> parameters = optionalTemplate.get().getParameters().build();
        assertEquals(6, parameters.size());

        assertEquals(STEP_INPUT_TABLE, parameters.get("input"));
        assertEquals(AUTH_BUNCH_INPUT_TABLE, parameters.get("outputPuid"));
        assertEquals(NO_AUTH_COUNT_TABLE, parameters.get("outputYuidCount"));
        assertEquals(33, parameters.get("sizeLimit"));
        assertNull(parameters.get("vars"));
        assertEquals(Map.of(), parameters.get("enums"));
    }
}
