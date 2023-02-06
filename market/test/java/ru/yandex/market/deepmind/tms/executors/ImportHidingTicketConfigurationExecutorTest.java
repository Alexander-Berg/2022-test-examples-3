package ru.yandex.market.deepmind.tms.executors;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketProcessing;
import ru.yandex.market.deepmind.common.hiding.configuration.HidingConfiguration;
import ru.yandex.market.deepmind.common.repository.HidingTicketProcessingRepository;

public class ImportHidingTicketConfigurationExecutorTest extends DeepmindBaseDbTestClass {
    @Resource
    private HidingTicketProcessingRepository hidingTicketProcessingRepository;

    private ImportHidingTicketConfigurationExecutor realExecutor;
    private ImportHidingTicketConfigurationExecutor executor;

    private EnhancedRandom random;
    private List<HidingConfiguration> hidingConfigurationList = new ArrayList<>();

    @Before
    public void setUp() {
        realExecutor = new ImportHidingTicketConfigurationExecutor(
            hidingTicketProcessingRepository
        );
        executor = Mockito.spy(realExecutor);
        Mockito.doAnswer(__ -> new ArrayList<>(hidingConfigurationList)).when(executor).getConfigurations();

        random = new EnhancedRandomBuilder().seed(1).build();
    }

    @Test
    public void testImportOne() {
        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("reason_key")
            .setSummary("my summary");
        hidingConfigurationList.add(config);

        executor.execute();

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("enabled", "reasonKey", "summary")
            .containsExactlyInAnyOrder(
                hidingTicketProcessing(false, "reason_key", "my summary")
            );
    }

    @Test
    public void testUpdateExisting() {
        hidingTicketProcessingRepository.save(
            hidingTicketProcessing(false, "reason_key", "my summary")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("reason_key")
            .setSummary("my summary 2");
        hidingConfigurationList.add(config);

        executor.execute();

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("enabled", "reasonKey", "summary")
            .containsExactlyInAnyOrder(
                hidingTicketProcessing(false, "reason_key", "my summary 2")
            );
    }

    @Test
    public void testUpdateExistingDontChangeEnabled() {
        hidingTicketProcessingRepository.save(
            hidingTicketProcessing(true, "reason_key", "my summary")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("reason_key")
            .setSummary("my summary 2");
        hidingConfigurationList.add(config);

        executor.execute();

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("enabled", "reasonKey", "summary")
            .containsExactlyInAnyOrder(
                hidingTicketProcessing(true, "reason_key", "my summary 2")
            );
    }

    @Test
    public void testDisableExisting() {
        hidingTicketProcessingRepository.save(
            new HidingTicketProcessing()
                .setEnabled(true)
                .setForceRun(true)
                .setReasonKey("reason_key")
                .setSummary("my summary")
        );

        executor.execute();

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("enabled", "forceRun", "reasonKey", "summary")
            .containsExactlyInAnyOrder(
                new HidingTicketProcessing()
                    .setEnabled(false)
                    .setForceRun(false)
                    .setReasonKey("reason_key")
                    .setSummary("my summary")
            );
    }

    @Test
    public void testUpdateAndInsertAndDisbaleExistingDontChangeEnabled() {
        // reason_key_1 to insert
        // reason_key_2 to update
        // reason_key_3 to disable
        hidingTicketProcessingRepository.save(
            hidingTicketProcessing(true, "reason_key_2", "summary2"),
            hidingTicketProcessing(true, "reason_key_3", "summary3")
        );

        hidingConfigurationList.add(random.nextObject(HidingConfiguration.class)
            .setReasonKey("reason_key_1").setSummary("summary"));
        hidingConfigurationList.add(random.nextObject(HidingConfiguration.class)
            .setReasonKey("reason_key_2").setSummary("summary 22"));

        executor.execute();

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("enabled", "reasonKey", "summary")
            .containsExactlyInAnyOrder(
                hidingTicketProcessing(false, "reason_key_1", "summary"),
                hidingTicketProcessing(true, "reason_key_2", "summary 22"),
                hidingTicketProcessing(false, "reason_key_3", "summary3")
            );
    }

    @Test
    public void testSecondRunWontChangeAnything() {
        // first run
        realExecutor.execute();

        var all1 = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all1).isNotEmpty();

        // second run
        realExecutor.execute();
        var all2 = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all2).containsExactlyInAnyOrderElementsOf(all1);
    }

    private static HidingTicketProcessing hidingTicketProcessing(boolean enabled, String reasonKey, String summary) {
        return new HidingTicketProcessing()
            .setEnabled(enabled)
            .setReasonKey(reasonKey)
            .setSummary(summary);
    }
}
