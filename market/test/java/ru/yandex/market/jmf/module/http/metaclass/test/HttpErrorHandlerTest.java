package ru.yandex.market.jmf.module.http.metaclass.test;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.http.metaclass.test.utils.HttpMetaclassUtils;
import ru.yandex.market.jmf.utils.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Transactional
@SpringJUnitConfig(classes = InternalModuleHttpMetaclassTestConfiguration.class)
public class HttpErrorHandlerTest {

    @Inject
    HttpMetaclassUtils utils;

    @Inject
    EntityStorageService entityStorageService;

    @Test
    public void testGetEmptyObjectErrorHandler() throws IOException {
        HttpStatusCodeException error = prepareErrorResponse(400);
        utils.prepareMocks("https://example.com/1", HttpMethod.GET, error);

        Fqn fqn = Fqn.of("testGetEmptyObjectErrorHandlerMetaclass");
        String gid = fqn.gidOf(1);

        Entity entity = entityStorageService.get(gid);

        assertNotNull(entity);
        assertEquals(gid, entity.getGid());
        assertEquals(fqn, entity.getMetaclass().getFqn());
    }

    @Test
    public void testGetDefaultErrorHandler() throws IOException {
        HttpStatusCodeException error = prepareErrorResponse(401);
        utils.prepareMocks("https://example.com/1", HttpMethod.GET, error);

        Fqn fqn = Fqn.of("testGetDefaultErrorHandlerMetaclass");
        String gid = fqn.gidOf(1);

        Entity entity = entityStorageService.get(gid);

        assertNotNull(entity);
        assertEquals(gid, entity.getGid());
        assertEquals(fqn, entity.getMetaclass().getFqn());
    }

    @Test
    public void testGetMessageErrorHandler() throws IOException {
        int statusCode = 502;
        HttpStatusCodeException error = prepareErrorResponse(statusCode);
        utils.prepareMocks("https://example.com/1", HttpMethod.GET, error);

        Fqn fqn = Fqn.of("testGetMessageErrorHandlerMetaclass");
        String gid = fqn.gidOf(1);

        DomainException exception = assertThrows(
                DomainException.class,
                () -> entityStorageService.get(gid)
        );
        assertEquals(
                "Http request for '%s' responded with: %d, body: null".formatted(gid, statusCode),
                exception.getMessage()
        );
        assertEquals("Ошибка при выполнении http запроса (%d)".formatted(statusCode), exception.getDisplayMessage());
    }

    @Test
    public void testListEmptyObjectErrorHandler() throws IOException {
        HttpStatusCodeException error = prepareErrorResponse(599);
        utils.prepareMocks("https://example.com/list", HttpMethod.GET, error);

        Fqn fqn = Fqn.of("testListEmptyObjectErrorHandlerMetaclass");

        List<Entity> entities = entityStorageService.list(Query.of(fqn));
        assertEquals(List.of(), entities);
    }

    @Test
    public void testListDefaultErrorHandler() throws IOException {
        HttpStatusCodeException error = prepareErrorResponse(403);
        utils.prepareMocks("https://example.com/list", HttpMethod.GET, error);

        Fqn fqn = Fqn.of("testListDefaultErrorHandlerMetaclass");

        List<Entity> entities = entityStorageService.list(Query.of(fqn));
        assertEquals(List.of(), entities);
    }

    @Test
    public void testListMessageErrorHandler() throws IOException {
        int statusCode = 403;
        HttpStatusCodeException error = prepareErrorResponse(statusCode);
        utils.prepareMocks("https://example.com/list", HttpMethod.GET, error);

        Fqn fqn = Fqn.of("testListMessageErrorHandlerMetaclass");

        DomainException exception = assertThrows(
                DomainException.class,
                () -> entityStorageService.list(Query.of(fqn))
        );
        assertEquals(
                "Http request for '%s' responded with: %d, body: null".formatted(fqn, statusCode),
                exception.getMessage()
        );
        assertEquals("Ошибка при выполнении http запроса (%d)".formatted(statusCode), exception.getDisplayMessage());
    }

    private HttpStatusCodeException prepareErrorResponse(int statusCode) throws IOException {
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
        when(exception.getRawStatusCode()).thenReturn(statusCode);
        when(exception.getResponseBodyAsByteArray()).thenReturn(null);
        when(exception.getResponseHeaders()).thenReturn(httpHeaders);

        return exception;
    }
}
