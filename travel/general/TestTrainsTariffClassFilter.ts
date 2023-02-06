import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import {TestTrainsTariffClassFilterItem} from './TestTrainsTariffClassFilterItem';

export class TestTrainsTariffClassFilter extends Component {
    toggleButton: Component | null;
    options: ComponentArray<TestTrainsTariffClassFilterItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.toggleButton = this.isTouch
            ? new Component(browser, {
                  parent: this.qa,
                  key: 'toggler',
              })
            : null;

        this.options = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'option',
            },
            TestTrainsTariffClassFilterItem,
        );
    }

    async clickFilter(value: string): Promise<void> {
        if (this.toggleButton) {
            await this.toggleButton.click();
        }

        const filter = await this.options.findSeries(async item => {
            const text = await item.text.getText();

            return text.toLowerCase() === value;
        });

        if (!filter) {
            throw new Error(`Не найден фильтр для выбора "${value}"`);
        }

        await filter.click();
    }
}
