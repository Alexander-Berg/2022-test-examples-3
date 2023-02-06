package ru.yandex.calendar.unistat;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.calendar.frontend.mailhook.AbstractMailhookTest;
import ru.yandex.calendar.frontend.mailhook.MailhookCallbackServlet;
import ru.yandex.calendar.frontend.mailhook.MailhookService;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

public class MailhookUnistatTest extends AbstractMailhookTest {
    @Autowired
    private MailhookCallbackServlet mailhookCallbackServlet;
    @Autowired
    private MailhookService mailhookService;

    @SneakyThrows
    @Test
    public void doPostWithExceptionTest() {
        val req = new MockHttpServletRequest("POST", "/mailhook");
        val resp = new MockHttpServletResponse();

        Mockito.doThrow(RuntimeException.class).when(mailhookService).execute(req, resp);

        mailhookCallbackServlet.service(req, resp);

        checkCounterValue("application.request.web./mailhook.error", 1.);
    }

    @SneakyThrows
    @Test
    public void doPostSuccessTest() {
        val req = new MockHttpServletRequest("POST", "/mailhook");
        val resp = new MockHttpServletResponse();

        Mockito.doNothing().when(mailhookService).execute(req, resp);

        mailhookCallbackServlet.service(req, resp);
        mailhookCallbackServlet.service(req, resp);

        checkCounterValue("application.request.web./mailhook.success", 2.);
    }
}
