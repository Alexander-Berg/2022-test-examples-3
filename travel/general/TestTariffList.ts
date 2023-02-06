import {TestTariffListItem} from 'helpers/project/trains/components/TestTariffListItem';

import {ComponentArray} from 'components/ComponentArray';

export class TestTariffList extends ComponentArray<TestTariffListItem> {
    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa, TestTariffListItem);
    }
}
