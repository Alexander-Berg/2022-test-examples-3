package ru.yandex.market.api.controller.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.abtest.ExperimentParameter;
import ru.yandex.market.api.abtest.Split;
import ru.yandex.market.api.abtest.UaasResponse;
import ru.yandex.market.api.controller.v2.startup.ActiveExperiment;
import ru.yandex.market.api.controller.v2.startup.ActiveExperimentChooser;
import ru.yandex.market.api.integration.BaseTest;

/**
 * @author dimkarp93
 */
public class ActiveExperimentChooserTest extends BaseTest {
    public static class TestListener implements ActiveExperimentChooser.Listener {
        private List<ActiveExperiment> experiments = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        @Override
        public void onConditionalFailed(ActiveExperiment experiment) {
            experiments.add(experiment);
        }

        @Override
        public void onError(String errorMessage) {
            errors.add(errorMessage);
        }

        public List<ActiveExperiment> getExperiments() {
            return experiments;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private TestListener listener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.listener = new TestListener();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        this.listener = null;
    }

    @Inject
    private ActiveExperimentChooser chooser;

    @Test
    public void chooseTest() {
        ExperimentParameter parameter1 = new ExperimentParameter();
        parameter1.setHandler("MARKET");
        parameter1.setTestIds(Collections.singleton("354515"));
        parameter1.setRearrFactors(
                Arrays.asList(
                        "market_show_hidden_suppliers=999321",
                        "market_hide_dsbs_by_cis_category=0",
                        "show_explicit_content"
                )
        );
        parameter1.setAliases(Collections.emptyList());

        ExperimentParameter parameter2 = new ExperimentParameter();
        parameter2.setHandler("MARKET");
        parameter2.setTestIds(Collections.singleton("354515"));
        parameter2.setRearrFactors(
                Arrays.asList(
                        "market_show_hidden_suppliers=999321",
                        "market_hide_dsbs_by_cis_category=0",
                        "show_explicit_content"
                )
        );
        parameter2.setAliases(Collections.emptyList());


        ExperimentParameter parameter3 = new ExperimentParameter();
        parameter3.setHandler("MARKETAPPS");
        parameter3.setTestIds(Collections.singleton("354515"));
        parameter3.setRearrFactors(
                Arrays.asList(
                        "market_show_hidden_suppliers=999321",
                        "market_hide_dsbs_by_cis_category=0",
                        "show_explicit_content"
                )
        );
        parameter3.setAliases(Collections.singleton("test_354515"));
        parameter3.setBackendExp(true);

        UaasResponse response = new UaasResponse(
                "1.0.0",
                Arrays.asList(
                        new Split("354515", "-1")
                ),
                Arrays.asList(
                        parameter1,
                        parameter2,
                        parameter3
                ),
                "0123"
        );

        Collection<ActiveExperiment> result = choose(response);
        Assert.assertThat(result, Matchers.hasSize(1));

        ActiveExperiment experiment = result.iterator().next();
        Assert.assertThat(experiment.getAlias(), Matchers.is("test_354515"));
    }

    private Collection<ActiveExperiment> choose(UaasResponse response) {
        return chooser.choose(response, Collections.emptyList(), listener);
    }
}
