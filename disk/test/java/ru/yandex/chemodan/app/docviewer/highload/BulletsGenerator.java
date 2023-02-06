package ru.yandex.chemodan.app.docviewer.highload;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.chemodan.app.docviewer.DocviewerAnnotationTestContextLoader;
import ru.yandex.chemodan.app.docviewer.config.ConvertersContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.CoreContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.TestContextConfiguration;
import ru.yandex.chemodan.app.docviewer.convert.result.PageInfo;
import ru.yandex.chemodan.app.docviewer.dao.results.StoredResult;
import ru.yandex.chemodan.app.docviewer.dao.results.StoredResultDao;

/**
 * @author akirakozov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = { ConvertersContextConfiguration.class, CoreContextConfiguration.class, TestContextConfiguration.class },
        loader=DocviewerAnnotationTestContextLoader.class)
public class BulletsGenerator {

    @Autowired
    private StoredResultDao storedResultDao;

    private void generate(Writer writer, int numOfDocuments) throws IOException {
        int counter = 0;
        for (StoredResult storedResult : storedResultDao.findByLastAccessLess(new Instant())) {
            counter++;
            if (counter > numOfDocuments) {
                break;
            }

            String fileId = storedResult.getFileId();
            if (storedResult.getPagesInfo().isPresent()) {
                for (PageInfo pageInfo : storedResult.getPagesInfo().get().getPageInfos()) {
                    int pageNum = pageInfo.getIndex().get();
                    writer.write("/htmlwithimagespageinfo?uid=0&width=900&id=" + fileId + "&page=" + pageNum + "\n");
                    writer.write("/htmlimage?width=900&id=" + fileId + "&name=bg-" + (pageNum -1) + ".png\n");
                }
            } else {
                writer.write("/htmlwithimagespageinfo?uid=0&width=900&id=" + fileId + "&page=1\n");
                writer.write("/htmlimage?width=900&id=" + fileId + "&name=bg-0.png\n");
            }
        }
    }

    @Test
    public void generateBullets() throws IOException {
        FileWriter fw = new FileWriter("bullets.txt");
        generate(fw, 1);
        fw.close();
    }
}
