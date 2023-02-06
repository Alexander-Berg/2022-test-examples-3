import {SECOND} from 'helpers/constants/dates';

import {Component} from 'components/Component';

export default class TestAviaSearchResultFog extends Component {
    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'aviaSearchResultFog');
    }

    /**
     * Подождать пока паранджа скроется
     */
    async waitUntilProcessed(): Promise<void> {
        await this.browser.waitUntil(
            async () => {
                try {
                    const attr = await this.getAttribute('data-active');

                    return attr === 'false';
                } catch (e) {
                    return false;
                }
            },
            {timeout: 3 * SECOND, timeoutMsg: 'Паранжа не скрылась'},
        );
    }
}
