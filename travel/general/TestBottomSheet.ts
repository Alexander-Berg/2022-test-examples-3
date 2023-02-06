import {delay} from 'helpers/project/common/delay';

import {Component} from 'components/Component';

export default class TestBottomSheet extends Component {
    private readonly overlaySelector: string;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.overlaySelector = '.Drawer-Overlay';
    }

    async close(): Promise<void> {
        /**
         * Если несколько BottomSheet - кликаем по всем подложкам
         */
        await this.browser.execute(selector => {
            const elements = document.querySelectorAll<HTMLElement>(selector);

            for (const element of elements) {
                element.click();
            }
        }, this.overlaySelector);

        /**
         * Ждем анимацию закрытия шторки
         */
        await delay(1000);
    }
}
