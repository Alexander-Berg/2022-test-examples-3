package ru.yandex.market.sberlog.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.sberlog.SberlogConfig;
import ru.yandex.market.sberlog.cipher.CipherService;
import ru.yandex.market.sberlog.dao.ManipulateSessionDao;

import java.util.Objects;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 22.04.19
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SberlogConfig.class})
public class CustomMonitoringControllerTest {

    @Autowired
    private ManipulateSessionDao manipulateSessionDao;

    private CustomMonitoringController customMonitoringController;

    @Before
    public void setUp() {
        this.customMonitoringController = new CustomMonitoringController(manipulateSessionDao);
    }

    @Test
    public void checkDb() {
        ResponseEntity<?> checkDbResponce = customMonitoringController.checkDb();
        Assert.assertEquals(200, checkDbResponce.getStatusCode().value());

        Assert.assertEquals("0;OK", Objects.requireNonNull(checkDbResponce.getBody()).toString());

        ManipulateSessionDao manipulateSessionDaoMockito = Mockito.mock(ManipulateSessionDao.class);
        Mockito.when(manipulateSessionDaoMockito.getVersion()).thenReturn(false);
        CustomMonitoringController customMonitoringControllerWithMock =
                new CustomMonitoringController(manipulateSessionDaoMockito);

        ResponseEntity<?> checkDbResponceWithMock = customMonitoringControllerWithMock.checkDb();
        Assert.assertEquals(500, checkDbResponceWithMock.getStatusCode().value());

        Assert.assertEquals("2;i can't connect to PGaaS",
                Objects.requireNonNull(checkDbResponceWithMock.getBody()).toString());
    }

    @Test
    public void exceptionCheckDb() {
        JdbcTemplate jdbcTemplateWithMock = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplateWithMock.queryForObject("SELECT version()", String.class))
                .thenThrow(new RuntimeException());

        CipherService cipherServiceWithMock = Mockito.mock(CipherService.class);
        ManipulateSessionDao manipulateSessionDaoMockito =
                new ManipulateSessionDao(jdbcTemplateWithMock, cipherServiceWithMock);
        CustomMonitoringController customMonitoringControllerWithMock =
                new CustomMonitoringController(manipulateSessionDaoMockito);

        ResponseEntity<?> checkDbResponceWithMock = customMonitoringControllerWithMock.checkDb();
        Assert.assertEquals(500, checkDbResponceWithMock.getStatusCode().value());

        Assert.assertEquals("2;i can't connect to PGaaS",
                Objects.requireNonNull(checkDbResponceWithMock.getBody()).toString());
    }
}
