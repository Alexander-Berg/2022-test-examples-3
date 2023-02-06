package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.function.Consumer;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;

public class ActualizationResponseConfiguration {

    private final ResultActionsContainer cartResultActionsContainer = new ResultActionsContainer();
    private Consumer<MultiCart> multiCartChecker;
    private int expectedCartReturnCode = 200;
    private boolean checkCartErrors = true;
    private Class<? extends Exception> expectedException;
    private boolean isExceptionExpected = false;

    public Consumer<MultiCart> getMultiCartChecker() {
        return multiCartChecker;
    }

    /**
     * Кастомные проверки корзины после /cart
     */
    public void setMultiCartChecker(Consumer<MultiCart> multiCartChecker) {
        this.multiCartChecker = multiCartChecker;
    }

    /**
     * Кастомные проверки "сырого" ответа на ручку /cart
     */
    public ResultActionsContainer resultActions() {
        return cartResultActionsContainer;
    }

    public int getExpectedCartReturnCode() {
        return expectedCartReturnCode;
    }

    public void setExpectedCartReturnCode(int expectedCartReturnCode) {
        this.expectedCartReturnCode = expectedCartReturnCode;
    }

    public boolean isExceptionExpected() {
        return isExceptionExpected;
    }

    public Class<? extends Exception> getExpectedException() {
        return expectedException;
    }

    public void setExpectedException(Class<? extends Exception> expectedException) {
        this.isExceptionExpected = true;
        this.expectedException = expectedException;
    }

    public boolean checkCartErrors() {
        return checkCartErrors;
    }

    public void setCheckCartErrors(boolean checkCartErrors) {
        this.checkCartErrors = checkCartErrors;
    }

}
