package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.function.Consumer;

import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;

public class CheckoutResponseConfiguration {

    private final ResultActionsContainer checkoutResultActionsContainer = new ResultActionsContainer();
    private Consumer<MultiCart> multiCartAction;
    private Consumer<MultiOrder> multiOrderChecker;
    private int expectedCheckoutReturnCode = 200;
    private ResultMatcher errorMatcher;
    private boolean useErrorMatcher = true;
    private boolean checkOrderCreateErrors = true;
    private Class<? extends Exception> expectedException;
    private boolean isExceptionExpected = false;

    public Consumer<MultiCart> getPrepareBeforeCheckoutAction() {
        return multiCartAction;
    }

    /**
     * Кастомное действия с мультикорзиной, после запроса /cart перед /checkout
     */
    public void setPrepareBeforeCheckoutAction(Consumer<MultiCart> multiCartAction) {
        this.multiCartAction = multiCartAction;
    }

    public Consumer<MultiOrder> getMultiOrderChecker() {
        return multiOrderChecker;
    }

    public void setMultiOrderChecker(Consumer<MultiOrder> multiOrderChecker) {
        this.multiOrderChecker = multiOrderChecker;
    }

    public ResultActionsContainer resultActions() {
        return checkoutResultActionsContainer;
    }

    public int getExpectedCheckoutReturnCode() {
        return expectedCheckoutReturnCode;
    }

    public void setExpectedCheckoutReturnCode(int expectedCheckoutReturnCode) {
        this.expectedCheckoutReturnCode = expectedCheckoutReturnCode;
    }

    public ResultMatcher getErrorMatcher() {
        return errorMatcher;
    }

    public void setErrorMatcher(ResultMatcher errorMatcher) {
        this.errorMatcher = errorMatcher;
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

    public boolean useErrorMatcher() {
        return useErrorMatcher;
    }

    public void setUseErrorMatcher(boolean useErrorMatcher) {
        this.useErrorMatcher = useErrorMatcher;
    }

    public boolean checkOrderCreateErrors() {
        return checkOrderCreateErrors;
    }

    public void setCheckOrderCreateErrors(boolean checkOrderCreateErrors) {
        this.checkOrderCreateErrors = checkOrderCreateErrors;
    }

}
