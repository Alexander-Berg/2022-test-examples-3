package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.common;

import io.qameta.allure.Step;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.should;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;

public class HtmlElementsCommonSteps {

    @Step("Click element {element}, when clickable")
    public static void safeClick(HtmlElement element) {
        assertThat(element, should(WrapsElementMatchers.isDisplayed()).whileWaitingUntil(timeoutHasExpired()));
        element.click();
    }

}


