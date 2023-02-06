package ru.yandex.market.mbo.mdm.common.service.stuff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.Assert.assertEquals;

public class StuffExistenceMonitorTest extends StuffServiceBaseTestClass {

    private static final String MONITORING_NAME = MdmStuffExistenceMonitor.class.getSimpleName();
    private static final String GOOD_DIR = "0bept1_d";
    private static final String BAD_DIR = "skrlekvnxiswjr";

    private MdmStuffExistenceMonitor stuffMonitor;
    private List<MdmStuffService> services;
    private Path goodPath;
    private Path badPath;
    private ScheduledExecutorService executor;

    @Before
    public void setUp() throws IOException {
        services = new ArrayList<>();
        executor = Executors.newSingleThreadScheduledExecutor();
        // Block executor from executing anything, otherwise tests might flap due to check inside stuffMonitor.init
        executor.submit(() -> ThreadUtils.sleep(1, TimeUnit.HOURS));
        stuffMonitor = new MdmStuffExistenceMonitor(executor, complexMonitoring, services, true);
        tempFolder.newFolder(GOOD_DIR);
        goodPath = Paths.get(tempFolder.getRoot().getAbsolutePath() + "/" + GOOD_DIR);
        badPath = Paths.get(tempFolder.getRoot().getAbsolutePath() + "/" + BAD_DIR);
    }

    @After
    public void cleanup() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void testEmptyServiceDoesNothing() {
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult(MONITORING_NAME).getStatus());
        stuffMonitor.init();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult(MONITORING_NAME).getStatus());
    }

    @Test
    public void testReadErrorDoesCrit() {
        DummyService dummyService = new DummyService(HasData.YES, goodPath, new RuntimeException("read failed"));

        Assertions.assertThatCode(dummyService::checkForUpdate)
            .hasMessageContaining("read failed");

        ComplexMonitoring.Result monitoringResult = DummyService.DUMMY_MONITORING.getResult();

        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        services.clear();
    }

    @Test
    public void testSingleServiceCase() {
        stuffMonitor.init();

        addService(HasData.YES, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, badPath); //тоже ок, т.к. данные всё ещё хороши
        expected(MonitoringStatus.OK);

        addService(HasData.OUTDATED, goodPath); //ок, т.к. если пути хорошие, то дальше не копаем
        expected(MonitoringStatus.OK);

        addService(HasData.OUTDATED, badPath);
        expected(MonitoringStatus.WARNING);

        addService(HasData.NO, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);
    }

    @Test
    public void testTwoServices() {
        stuffMonitor.init();

        //Шесть персон, задача о рукопожатиях.

        //Сервис с данными + хорошим путём vs остальные:
        addService(HasData.YES, goodPath);
        addService(HasData.YES, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, goodPath);
        addService(HasData.YES, badPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, goodPath);
        addService(HasData.OUTDATED, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, goodPath);
        addService(HasData.OUTDATED, badPath);
        expected(MonitoringStatus.WARNING);

        addService(HasData.YES, goodPath);
        addService(HasData.NO, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, goodPath);
        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);


        //Сервис с данными + несуществующим путём vs остальные:
        addService(HasData.YES, badPath);
        addService(HasData.YES, badPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, badPath);
        addService(HasData.OUTDATED, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, badPath);
        addService(HasData.OUTDATED, badPath);
        expected(MonitoringStatus.WARNING);

        addService(HasData.YES, badPath);
        addService(HasData.NO, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.YES, badPath);
        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);


        //Сервис с устаревшими данными и хорошим путём vs остальные:
        addService(HasData.OUTDATED, goodPath);
        addService(HasData.OUTDATED, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.OUTDATED, goodPath);
        addService(HasData.OUTDATED, badPath);
        expected(MonitoringStatus.WARNING);

        addService(HasData.OUTDATED, goodPath);
        addService(HasData.NO, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.OUTDATED, goodPath);
        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);


        //Сервис с устаревшими данными и несуществующей директорией:
        addService(HasData.OUTDATED, badPath);
        addService(HasData.OUTDATED, badPath);
        expected(MonitoringStatus.WARNING);

        addService(HasData.OUTDATED, badPath);
        addService(HasData.NO, goodPath);
        expected(MonitoringStatus.WARNING);

        addService(HasData.OUTDATED, badPath);
        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);

        //Сервис без данных, но с хорошим путём:
        addService(HasData.NO, goodPath);
        addService(HasData.NO, goodPath);
        expected(MonitoringStatus.OK);

        addService(HasData.NO, goodPath);
        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);

        //Позор семьи - сервис без данных и с несущ. путём (забился в угол и здоровается только сам с собой):
        addService(HasData.NO, badPath);
        addService(HasData.NO, badPath);
        expected(MonitoringStatus.CRITICAL);
    }

    @Test
    public void testMonitoringUpdatesFromGoodToBad() throws IOException {
        stuffMonitor.init();
        addService(HasData.NO, goodPath);
        stuffMonitor.checkIfStuffPathsExist();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult(MONITORING_NAME).getStatus());

        Files.delete(goodPath);
        stuffMonitor.checkIfStuffPathsExist();
        assertEquals(MonitoringStatus.CRITICAL, complexMonitoring.getResult(MONITORING_NAME).getStatus());
    }

    @Test
    public void testMonitoringUpdatesFromBadToGood() throws IOException {
        stuffMonitor.init();
        addService(HasData.NO, badPath);
        stuffMonitor.checkIfStuffPathsExist();
        assertEquals(MonitoringStatus.CRITICAL, complexMonitoring.getResult(MONITORING_NAME).getStatus());

        tempFolder.newFolder(BAD_DIR);
        stuffMonitor.checkIfStuffPathsExist();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult(MONITORING_NAME).getStatus());
    }

    private void expected(MonitoringStatus status) {
        stuffMonitor.checkIfStuffPathsExist();
        assertEquals(status, complexMonitoring.getResult(MONITORING_NAME).getStatus());
        services.clear();
    }

    private void addService(HasData hasData, Path path) {
        services.add(new DummyService(hasData, path));
    }

    private enum HasData {
        NO,
        OUTDATED,
        YES
    }

    private static class DummyService extends MdmStuffService {

        private static final ComplexMonitoring DUMMY_MONITORING = new ComplexMonitoring();
        private final RuntimeException readException;
        private HasData hasData;
        private Path path;

        DummyService(HasData hasData, Path path) {
            this(hasData, path, null);
        }

        DummyService(HasData hasData, Path path, RuntimeException readException) {
            super(null, DUMMY_MONITORING.createUnit(UUID.randomUUID().toString()), 1,
                true);

            this.hasData = hasData;
            this.path = path;
            this.readException = readException;
        }

        @Override
        protected Path getExportDirPath() {
            return path;
        }

        @Override
        protected boolean hasData() {
            return hasData != HasData.NO;
        }

        @Override
        protected void read() {
            if (readException != null) {
                throw readException;
            }
        }

        @Override
        public boolean isOutdatedExportLoaded() {
            return hasData == HasData.OUTDATED;
        }
    }

}
