import {Component} from 'components/Component';
import {Button} from 'components/Button';

import {TestTrainsTariffClassFilter} from './TestTrainsTariffClassFilter';

export class TestTrainsMobileFilters extends Component {
    toggleButton: Component;
    tariffClass: TestTrainsTariffClassFilter;
    applyButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'mobileFilters') {
        super(browser, qa);

        this.toggleButton = new Component(browser, {
            parent: this.qa,
            current: 'toggleButton',
        });
        this.tariffClass = new TestTrainsTariffClassFilter(browser, {
            parent: this.qa,
            current: 'trainTariffClass',
        });
        this.applyButton = new Button(browser, {
            parent: this.qa,
            current: 'applyButton',
        });
    }

    async setFilter(type: 'tariffClass', value: string): Promise<void> {
        await this.toggleButton.click();

        await this[type].clickFilter(value);

        await this.applyButton.click();
    }
}
