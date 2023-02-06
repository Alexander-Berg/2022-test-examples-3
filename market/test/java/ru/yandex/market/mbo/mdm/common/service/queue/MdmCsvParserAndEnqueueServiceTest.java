package ru.yandex.market.mbo.mdm.common.service.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.amazonaws.util.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueWithInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueWithInfo.QueueKeyType.MSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueWithInfo.QueueKeyType.SSKU_KEY;

public class MdmCsvParserAndEnqueueServiceTest extends MdmBaseDbTestClass {

    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;

    private MdmCsvParserAndEnqueueService csvParserAndEnqueueService;

    @Before
    public void setUp() {
        csvParserAndEnqueueService = new MdmCsvParserAndEnqueueService(List.of(
            new MdmQueueWithInfo("SSKU золото", SSKU_KEY,
                (keys, priority) -> sskuToRefreshRepository.enqueueAll(keys, MdmEnqueueReason.DEFAULT, priority)),
            new MdmQueueWithInfo("MSKU золото", MSKU_ID,
                (keys, priority) -> mskuToRefreshRepository.enqueueAll(keys, MdmEnqueueReason.DEFAULT, priority))
        ));
    }

    @Test
    public void testImportCsvForEnqueue() {
        Assertions.assertThat(csvParserAndEnqueueService.getQueueSet())
            .containsExactlyInAnyOrder("SSKU золото", "MSKU золото");
    }

    @Test
    public void whenImportFileWithSskuShouldInsertSskuInQueue() {
        URL resource = getClass().getClassLoader().getResource("ssku_for_enqueue.csv");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        try (FileInputStream fis = new FileInputStream(new File(resource.toURI()));) {
            byte[] fileBytes = IOUtils.toByteArray(fis);
            MultipartFile file = new MockMultipartFile("ssku_for_enqueue.csv", fileBytes);
            csvParserAndEnqueueService.importCsvForEnqueue(file, "SSKU золото", 12);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        Assertions.assertThat(sskuToRefreshRepository.findAll())
            .hasSize(4)
            .allMatch(info -> info.getPriority() == 12);
    }

    @Test
    public void whenImportFileWithMskuShouldInsertMskuInQueue() {
        URL resource = getClass().getClassLoader().getResource("msku_for_enqueue.csv");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        try (FileInputStream fis = new FileInputStream(new File(resource.toURI()))) {
            byte[] fileBytes = IOUtils.toByteArray(fis);
            MultipartFile file = new MockMultipartFile("msku_for_enqueue.csv", fileBytes);
            csvParserAndEnqueueService.importCsvForEnqueue(file, "MSKU золото", 12345);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        Assertions.assertThat(mskuToRefreshRepository.findAll())
            .hasSize(4)
            .allMatch(info -> info.getPriority() == 12345);
    }
}
