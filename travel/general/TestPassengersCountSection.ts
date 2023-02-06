import {Component} from 'components/Component';

import {TestPassengersCountSelector} from './TestPassengersCountSelector';

export class TestPassengersCountSection extends Component {
    title: Component;
    subtitle: Component;
    adults: TestPassengersCountSelector;
    children: TestPassengersCountSelector;
    babies: TestPassengersCountSelector;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

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
        const passengersCount = this[type].passengersCount;

        await passengersCount.scrollIntoView();
        await passengersCount.setCount(count);
    }
}
