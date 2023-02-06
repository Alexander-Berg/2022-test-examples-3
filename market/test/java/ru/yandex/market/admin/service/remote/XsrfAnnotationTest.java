package ru.yandex.market.admin.service.remote;

import java.util.Arrays;

import com.google.gwt.user.server.rpc.XsrfProtect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ru.yandex.common.magic.service.XsrfMagicRemoteServiceServlet;
import ru.yandex.market.admin.FunctionalTest;

class XsrfAnnotationTest extends FunctionalTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    @DisplayName("Проверка наличия аннотаций у RemoteService интерфейсов")
    void testXsrfAnnotation() {
        Assertions.assertTrue(applicationContext.getBeansOfType(XsrfMagicRemoteServiceServlet.class).values().stream()
                .map(XsrfMagicRemoteServiceServlet::getClass)
                .allMatch(v -> Arrays
                        .stream(v.getInterfaces())
                        .anyMatch(l -> l.isAnnotationPresent(XsrfProtect.class))
                ));
    }
}
