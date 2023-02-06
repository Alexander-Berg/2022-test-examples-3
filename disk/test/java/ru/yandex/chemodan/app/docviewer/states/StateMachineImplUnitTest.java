package ru.yandex.chemodan.app.docviewer.states;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.convert.ConvertManager;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.dao.results.StoredResult;
import ru.yandex.chemodan.app.docviewer.dao.results.StoredResultDao;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUri;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUriDao;
import ru.yandex.misc.test.Assert;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 *
 * @author akirakozov
 */
public class StateMachineImplUnitTest {

    private static final TargetType TEST_TARGET = TargetType.HTML_WITH_IMAGES;
    private static final ActualUri TEST_URI = new ActualUri("http://some.test.url/file.doc");
    private static final String TEST_FILE_ID = new String("file-id");

    private StateMachineImpl sut;

    @Mock
    private ConvertManager convertManager;

    @Mock
    private StoredResultDao storedResultDao;

    @Mock
    private StoredUriDao storedUriDao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sut = new StateMachineImpl();
        sut.setConvertManager(convertManager);
        sut.setStoredResultDao(storedResultDao);
        sut.setStoredUriDao(storedUriDao);
    }

    @Test
    public void checkNotStarted() {
        when(storedUriDao.find(eq(TEST_URI))).thenReturn(Option.empty());

        Assert.equals(State.NOT_STARTED, sut.getState(TEST_URI, TEST_TARGET));
    }

    @Test
    public void checkCopying() {
        when(storedUriDao.find(eq(TEST_URI))).thenReturn(Option.of(new StoredUri()));

        Assert.equals(State.COPYING, sut.getState(TEST_URI, TEST_TARGET));
    }

    @Test
    public void checkNotStartedForTarget() {
        StoredUri storedUri = new StoredUri();
        storedUri.setFileId(Option.of(TEST_FILE_ID));
        when(storedUriDao.find(eq(TEST_URI))).thenReturn(Option.of(storedUri));

        Assert.equals(State.NOT_STARTED, sut.getState(TEST_URI, TEST_TARGET));
    }

    @Test
    public void checkConverting() {
        StoredUri storedUri = new StoredUri();
        storedUri.setFileId(Option.of(TEST_FILE_ID));
        storedUri.setConvertTargets(Cf.map(TEST_TARGET, 1.f));

        when(storedUriDao.find(eq(TEST_URI))).thenReturn(Option.of(storedUri));
        when(storedResultDao.find(eq(TEST_FILE_ID), eq(TEST_TARGET))).thenReturn(Option.empty());
        when(convertManager.isScheduled(eq(TEST_FILE_ID), eq(TEST_TARGET))).thenReturn(true);

        Assert.equals(State.CONVERTING, sut.getState(TEST_URI, TEST_TARGET));
    }

    @Test
    public void checkCopied() {
        StoredUri storedUri = new StoredUri();
        storedUri.setFileId(Option.of(TEST_FILE_ID));
        storedUri.setConvertTargets(Cf.map(TEST_TARGET, 1.f));

        when(storedUriDao.find(eq(TEST_URI))).thenReturn(Option.of(storedUri));
        when(convertManager.isScheduled(eq(TEST_FILE_ID), eq(TEST_TARGET))).thenReturn(false);
        when(storedResultDao.find(eq(TEST_FILE_ID), eq(TEST_TARGET))).thenReturn(Option.empty());

        Assert.equals(State.COPIED, sut.getState(TEST_URI, TEST_TARGET));
    }


    @Test
    public void checkAvailable() {
        StoredUri storedUri = new StoredUri();
        storedUri.setFileId(Option.of(TEST_FILE_ID));
        storedUri.setConvertTargets(Cf.map(TEST_TARGET, 1.f));

        StoredResult result = new StoredResult();
        result.setFileId(TEST_FILE_ID);

        when(storedUriDao.find(eq(TEST_URI))).thenReturn(Option.of(storedUri));
        when(storedResultDao.find(eq(TEST_FILE_ID), eq(TEST_TARGET))).thenReturn(Option.of(result));

        Assert.equals(State.AVAILABLE, sut.getState(TEST_URI, TEST_TARGET));
    }

}
