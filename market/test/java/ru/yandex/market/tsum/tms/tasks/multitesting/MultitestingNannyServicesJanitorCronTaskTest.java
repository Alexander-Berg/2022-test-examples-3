package ru.yandex.market.tsum.tms.tasks.multitesting;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.tsum.multitesting.MultitestingDatacenterWeightService;
import ru.yandex.market.tsum.multitesting.model.JanitorOptions;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 29.05.2018
 */
public class MultitestingNannyServicesJanitorCronTaskTest {
    @Test
    public void getNannyServicesWithoutEnvironments() {
        assertEquals(
            Collections.singletonList("mt_5--6_hashsum_iva"),
            MultitestingNannyServicesJanitorCronTask.getNannyServicesWithoutEnvironments(
                asList("mt_1--2_hashsum_iva", "mt_3--4_hashsum_iva", "mt_5--6_hashsum_iva"),
                asList(archivedEnvironment("1--2"), idleEnvironment("3--4"))
            )
        );
    }

    @Test
    public void getNannyServicesWithArchivedEnvironments() {
        assertEquals(
            Collections.singletonList("mt_1--2_hashsum_iva"),
            MultitestingNannyServicesJanitorCronTask.getNannyServicesWithArchivedEnvironments(
                asList("mt_1--2_hashsum_iva", "mt_3--4_hashsum_iva", "mt_5--6_hashsum_iva"),
                asList(archivedEnvironment("1--2"), idleEnvironment("3--4"))
            )
        );
    }

    @Test
    public void getNannyServicesWithEnvironmentsInDifferentLocations() {
        assertEquals(
            asList("mt_1--2_hashsum_iva", "mt_3--4_hashsum_sas"),
            MultitestingNannyServicesJanitorCronTask.getNannyServicesWithEnvironmentsInDifferentLocations(
                asList("mt_1--2_hashsum_iva", "mt_1--2_hashsum_sas", "mt_3--4_hashsum_iva", "mt_3--4_hashsum_sas"),
                asList(activeEnvironment("1--2", "SAS"), activeEnvironment("3--4", "IVA"))
            )
        );
    }

    private static MultitestingEnvironment idleEnvironment(String id) {
        return environment(id, MultitestingEnvironment.Status.IDLE);
    }

    private static MultitestingEnvironment activeEnvironment(String id, String dc) {
        return environment(id, dc, MultitestingEnvironment.Status.READY);
    }

    private static MultitestingEnvironment archivedEnvironment(String id) {
        return environment(id, MultitestingEnvironment.Status.ARCHIVED);
    }

    private static MultitestingEnvironment environment(String id, MultitestingEnvironment.Status status) {
        return environment(id, MultitestingDatacenterWeightService.DEFAULT_DC, status);
    }

    private static MultitestingEnvironment environment(String id, String dc, MultitestingEnvironment.Status status) {
        return new MultitestingEnvironment(
            id,
            null,
            null,
            0,
            null,
            null,
            null,
            status,
            dc,
            null,
            null,
            null,
            null,
            false,
            JanitorOptions.DEFAULT_OPTIONS,
            null);
    }
}
