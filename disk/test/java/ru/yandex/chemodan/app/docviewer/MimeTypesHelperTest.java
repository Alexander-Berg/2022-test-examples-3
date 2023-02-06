package ru.yandex.chemodan.app.docviewer;

import org.junit.Test;

import ru.yandex.bolts.collection.SetF;
import ru.yandex.misc.test.Assert;


/**
 * @author Vsevolod Tolstopyatov (qwwdfsad)
 */
public class MimeTypesHelperTest {

    /**
     * This is not 'real' test. The only purpose of this test
     * is to check that implementation is partially correct (returns enough mime types) and doesn't throw any exception
     */
    @Test
    public void testGetAllMimeTypes() {
        SetF<String> allMimeTypes = MimeTypesHelper.getAllMimeTypes();
        Assert.ge(allMimeTypes.size(), 100);
    }
}
