package ru.yandex.chemodan.uploader.social;

import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.uploader.ChemodanService;
import ru.yandex.misc.thread.SemaphoreNotAvailableException;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
public class ExternalResourceSemaphoresTest {

    private Function0 mock = Mockito.mock(Function0.class);

    @Test
    public void shouldExecuteWithSemaphore() {
        ChemodanService service = ChemodanService.VKONTAKTE;
        Properties properties = new Properties();
        properties.setProperty(ExternalResourceSemaphores.SEMAPHORES_PROPERTY + service, "1");
        ExternalResourceSemaphores sut = new ExternalResourceSemaphores(properties);

        sut.executeWithSemaphore(service, mock);

        verify(mock, only()).apply();
    }

    @Test
    public void shouldExecuteWithoutSemaphore() {
        ExternalResourceSemaphores sut = new ExternalResourceSemaphores(new Properties());
        sut.executeWithSemaphore(ChemodanService.FACEBOOK, mock);

        verify(mock, only()).apply();
    }

    @Test(expected = SemaphoreNotAvailableException.class)
    public void shouldThrowExceptionWhenSemaphoreIsFull() {
        ChemodanService service = ChemodanService.VKONTAKTE;
        Semaphore semaphoreMock = Mockito.mock(Semaphore.class);
        MapF<ChemodanService, Semaphore> semaphores = Cf.map(service, semaphoreMock);
        ExternalResourceSemaphores sut = new ExternalResourceSemaphores(semaphores);

        when(semaphoreMock.tryAcquire()).thenReturn(false);
        sut.executeWithSemaphore(service, mock);
    }

    @Test
    public void shouldExecuteMultipleTimes() {
        ChemodanService service = ChemodanService.FACEBOOK;
        Semaphore semaphoreMock = Mockito.mock(Semaphore.class);
        MapF<ChemodanService, Semaphore> semaphores = Cf.map(service, semaphoreMock);
        ExternalResourceSemaphores sut = new ExternalResourceSemaphores(semaphores);

        when(semaphoreMock.tryAcquire()).thenReturn(true).thenReturn(true);
        sut.executeWithSemaphore(service, mock);
        sut.executeWithSemaphore(service, mock);

        verify(semaphoreMock, times(2)).tryAcquire();
        verify(semaphoreMock, times(2)).release();
    }

    @Test(expected = SemaphoreNotAvailableException.class)
    public void shouldThrowExceptionWhenDefaultSemaphoreIsFull() {
        Properties properties = new Properties();
        properties.setProperty(ExternalResourceSemaphores.DEFAULT_SEMAPHORES_PROPERTY, "0");
        ExternalResourceSemaphores sut = new ExternalResourceSemaphores(properties);

        sut.executeWithSemaphore(ChemodanService.INSTAGRAM, mock);
    }

    @Test
    public void shouldUseConcreteSemaphoreInsteadOfDefaultIfDefined() {
        ChemodanService service = ChemodanService.INSTAGRAM;
        Properties properties = new Properties();
        properties.setProperty(ExternalResourceSemaphores.DEFAULT_SEMAPHORES_PROPERTY, "0");
        properties.setProperty(ExternalResourceSemaphores.SEMAPHORES_PROPERTY + service, "1");
        ExternalResourceSemaphores sut = new ExternalResourceSemaphores(properties);

        sut.executeWithSemaphore(service, mock);

        verify(mock, only()).apply();
    }
}
