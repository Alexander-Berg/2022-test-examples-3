import {MINUTE} from 'helpers/constants/dates';

import {TestModal} from 'helpers/project/common/components/TestModal';
import {Component} from 'helpers/project/common/components/Component';

const AWAIT_VISIBLE_CANCEL_BUTTON_TIMEOUT = MINUTE;

export class TestHotelsCancelOrderModal extends TestModal {
    buttonSubmitCancelOrder: Component;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.buttonSubmitCancelOrder = new Component(browser, {
            path: [this.qa],
            current: 'buttonSubmitCancelOrder',
        });
    }

    async buttonSubmitClick(): Promise<void> {
        await this.buttonSubmitCancelOrder.waitForVisible(
            AWAIT_VISIBLE_CANCEL_BUTTON_TIMEOUT,
        );

        /**
         * Иногда клик проходит по лоадеру, хоть и дожидаемся отображения кнопки,
         * поэтому еще немного ждем
         */
        await this.browser.pause(1000);

        await this.buttonSubmitCancelOrder.click();
    }
}
