import {Component} from 'components/Component';

import {TestPassengersCountSelector} from './TestPassengersCountSelector';

export class TestPassengersCountSection extends Component {
    subtitle: Component;
    adults: TestPassengersCountSelector;
    children: TestPassengersCountSelector;
    babies: TestPassengersCountSelector;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.subtitle = new Component(browser, {
            parent: this.qa,
            current: 'subtitle',
        });

        this.adults = new TestPassengersCountSelector(browser, {
            parent: this.qa,
            current: 'passengersCountSelector',
            key: 'adults',
        });

        this.children = new TestPassengersCountSelector(browser, {
            parent: this.qa,
            current: 'passengersCountSelector',
            key: 'children',
        });

        this.babies = new TestPassengersCountSelector(browser, {
            parent: this.qa,
            current: 'passengersCountSelector',
            key: 'babies',
        });
    }

    async selectPassengerCountByType(
        type: 'adults' | 'children' | 'babies',
        count: number,
    ): Promise<void> {
        const select = this[type].select;

        await select.trigger.click();

        await (await select.options.items)[count].click();
    }
}
