package ru.yandex.market.shopadminstub.test;

import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;

import static org.mockito.Mockito.withSettings;

/**
 * @author Nicolai Iusiumbeli <armor@yandex-team.ru>
 * date: 09/02/2017
 */
public class AbstractMockedUnitTestBase {
    static {
//        DOMConfigurator.configure(AbstractMockedUnitTest.class.getClassLoader().getResource("log4j-config.xml"));
    }

    protected static <T> T createMock(Class<T> clazz) {
        return Mockito.mock(clazz,
                withSettings().defaultAnswer(new ThrowsException(
                        new RuntimeException(clazz.getSimpleName() + "'s method is not defined")
                ))
        );
    }
}
