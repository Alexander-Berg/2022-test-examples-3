import {Component} from 'components/Component';
import {TestCheckButton} from 'components/TestCheckButton';

import {TestTrainsTariffClassFilter} from './TestTrainsTariffClassFilter';

export class TestTrainsInlineFilters extends Component {
    tariffClass: TestTrainsTariffClassFilter;

    priceTrigger: TestCheckButton;
    tariffClassTrigger: TestCheckButton;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'inlineFilters') {
        super(browser, qa);

        this.tariffClassTrigger = new TestCheckButton(browser, {
            parent: this.qa,
            current: 'trainTariffClassTrigger',
        });
        this.tariffClass = new TestTrainsTariffClassFilter(browser, {
            parent: this.qa,
            current: 'trainTariffClass',
        });

        this.priceTrigger = new TestCheckButton(browser, {
            parent: this.qa,
            current: 'priceTrigger',
        });
    }

    async setFilter(type: 'tariffClass', value: string): Promise<void> {
        await this.tariffClassTrigger.click();
        await this[type].clickFilter(value);
        // скрываем фильтр, чтобы не перекрывал контент
        await this.tariffClassTrigger.click();
    }
}
