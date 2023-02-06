package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.configuration.properties.RevisionProperties;
import ru.yandex.market.logistics.tarifficator.model.enums.FileStatus;
import ru.yandex.market.logistics.tarifficator.service.mds.MdsFileService;
import ru.yandex.market.logistics.tarifficator.service.revision.RevisionItemService;
import ru.yandex.market.logistics.tarifficator.service.tariff.TariffService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Интеграционный тест MarkForDeleteExpiredDatasetFilesExecutor")
class MarkForDeleteExpiredDatasetFilesExecutorTest extends AbstractContextualTest {

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    private RevisionItemService revisionItemService;

    @Autowired
    private TariffService tariffService;

    @Autowired
    private RevisionProperties revisionProperties;

    private MarkForDeleteExpiredDatasetFilesExecutor expiredDatasetFilesExecutor;

    @BeforeEach
    public void before() {
        expiredDatasetFilesExecutor = new MarkForDeleteExpiredDatasetFilesExecutor(
            revisionItemService,
            tariffService,
            mdsFileService,
            revisionProperties.getFilesCountToRetain(),
            revisionProperties.getTariffsCountToProcess()
        );
    }

    @AfterEach
    public void after() {
        verifyNoMoreInteractions(mdsFileService);
    }

    @Test
    @DisplayName("Поиск лишних файлов - файл используется для старой и новой ревизии")
    @DatabaseSetup("/tms/find-wasted-mds-files/before/one-file-for-old-and-new-revision.xml")
    @ExpectedDatabase(
        value = "/tms/find-wasted-mds-files/after/one-file-for-old-and-new-revision.xml",
        assertionMode = NON_STRICT
    )
    void successMarkForDeleteOneFileForOldAndNewRevision() {
        expiredDatasetFilesExecutor.doJob(null);

        /*
           filesCountToRetain = 1
           Тарифы - t1
           Прайслисты - p1(t1), p2(t1)
           Поколения - r1, r2, r3
           Элементы поколений - ri_1(r1, p1), ri_2(r2, p2), ri_3(r3, p1), ri_4(r2, p1)
           Датасет-файлы - f1(ri_1), f2(ri_2), f1(ri_3), f3(ri_4)

           Получается, соответствие:
                r1  r2  r3
           ------------------
           p1   f1  f3  f1
           p2       f2

           Файл f1 не должен удалиться,
           Файл f3 - должен
           Объяснение: нужно удалить f1 тк он устарел, но f1 также есть в новой ревизии, значит не удаляем.
           Файл f3 слишком старый (т.к. filesCountToRetain = 1) - удаляем
         */
        verify(mdsFileService).setStatus(List.of(3L), FileStatus.WAIT_DELETE_BINARY);
    }

    @Test
    @DisplayName("Поиск лишних файлов - все файлы найдены и помечены для удаления")
    @DatabaseSetup("/tms/find-wasted-mds-files/before/delete-xml-for-different-price-lists.xml")
    @ExpectedDatabase(
        value = "/tms/find-wasted-mds-files/after/delete-xml-for-different-price-lists.xml",
        assertionMode = NON_STRICT
    )
    void successMarkForDelete() {
        expiredDatasetFilesExecutor.doJob(null);

        /*
           filesCountToRetain = 1
           Тарифы - t1
           Прайслисты - p1(t1), p2(t1)
           Поколения - r1, r2, r3
           Элементы поколений - ri_1(r1, p1), ri_2(r2, p2), ri_3(r3, p1), ri_4(r3, p2)
           Датасет-файлы - f1(ri_1), f2(ri_2), f3(ri_3), f4(ri_4)

           Получается, соответствие:
                r1  r2  r3
           ------------------
           p1   f1      f3
           p2       f2  f4


           Объяснение: Файлы f1, f2 удаляются - слишком старые (т.к. filesCountToRetain = 1)
         */
        verify(mdsFileService).setStatus(List.of(1L, 2L), FileStatus.WAIT_DELETE_BINARY);
    }
}
