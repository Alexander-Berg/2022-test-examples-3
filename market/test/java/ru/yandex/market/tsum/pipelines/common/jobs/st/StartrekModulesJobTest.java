package ru.yandex.market.tsum.pipelines.common.jobs.st;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.tsum.pipelines.common.resources.StartrekComponents;

/**
 * @author: belmatter
 */
public class StartrekModulesJobTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final StartrekComponentsJob stModulesAdapter = new StartrekComponentsJob();

    private static final StartrekComponentsJob.ComponentsMappingResource COMPONENTS =
        StartrekComponentsJob.mapComponentToModule("MBI mbi-billing Биллинг", "mbi-billing")
            .addComponent("MBI mbi-db", "mbi-db")
            .addComponent("MBI partner-api Партнерский API", "partner-api")
            .addComponent("MBI market-payment ПИ - Бэкенд", "mbi-partner")
            .addComponent("MBI mbi-api Внутренний API для MBI", "mbi-api")
            .addComponent("MBI shopinfo Правовая информация о магазинах", "shopinfo")
            .addComponent("MBI mbi-admin Админка маркета - Логика", "mbi-admin")
            .addComponent("MBI mbi-data-archiver", "mbi-data-archiver")
            .addComponent("MBI report-generator", "report-generator")
            .addComponent("MBI mbi-bidding Ставки", "mbi-bidding")
            .addComponent("mbi-bidding-vendor", "mbi-bidding-vendor")
            .addComponent("MBI mbi-premoderation", "mbi-premoderation")
            .addComponent("mbi-xls2csv", "mbi-xls2csv");

    private static final StartrekComponentsJob.IgnoredComponentsResource IGNORED_COMPONENTS =
        StartrekComponentsJob.ignore("MBI mbi-api-client Клиент для mbi-api");


    @Before
    public void initJob() {
        stModulesAdapter.setComponentsResource(COMPONENTS);
        stModulesAdapter.setIgnoredComponents(IGNORED_COMPONENTS);
    }

    @Test
    public void testEmptyModules() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Set of components in release ticket must not be empty");
        stModulesAdapter.getModules(new StartrekComponents(Collections.emptySet()));
    }

    @Test
    public void testEmptyFilteredModules() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Set of components in release ticket must not be empty");
        stModulesAdapter.getModules(new StartrekComponents(Collections.singleton("MBI mbi-api-client Клиент для " +
            "mbi-api")));
    }

    @Test
    public void testUnknownComponent() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown startrek component: test");
        stModulesAdapter.getModules(new StartrekComponents(Collections.singleton("test")));
    }

    @Test
    public void testCorrectScenario() {
        Set<String> components = ImmutableSet.of(
            "MBI mbi-api-client Клиент для mbi-api",
            "mbi-xls2csv",
            "MBI partner-api Партнерский API",
            "MBI mbi-bidding Ставки"
        );
        List<String> modules = stModulesAdapter.getModules(new StartrekComponents(components));
        Assert.assertThat(modules,
            Matchers.containsInAnyOrder("mbi-xls2csv", "partner-api", "mbi-bidding"));
    }

}
