package ru.yandex.market.sc.core.test;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;

/**
 * @author valter
 */
public class InvalidateMemcachedExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        var memCachedClientFactoryMock = SpringExtension.getApplicationContext(context).getBean(
                "memCachedClientFactoryMock", MemCachedClientFactoryMock.class);
        memCachedClientFactoryMock.close();
    }

}
