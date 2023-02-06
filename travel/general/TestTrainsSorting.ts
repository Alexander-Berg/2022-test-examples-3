import {Component} from 'helpers/project/common/components/Component';

export class TestTrainsSorting extends Component {
    private readonly sortByPrice: Component;
    private readonly sortByDeparture: Component;
    private readonly sortByArrival: Component;
    private readonly sortByDuration: Component;

    private type: 'departure' | 'arrival' | 'price' | 'duration';

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.type = 'departure';

        this.sortByPrice = new Component(browser, {
            parent: this.qa,
            key: 'price',
        });

        this.sortByDeparture = new Component(browser, {
            parent: this.qa,
            key: 'departure',
        });

        this.sortByArrival = new Component(browser, {
            parent: this.qa,
            key: 'arrival',
        });

        this.sortByDuration = new Component(browser, {
            parent: this.qa,
            key: 'duration',
        });
    }

    async setSortBy(
        type: 'departure' | 'arrival' | 'price' | 'duration',
    ): Promise<void> {
        this.type = type;

        switch (type) {
            case 'price':
                return this.sortByPrice.click();
            case 'departure':
                return this.sortByDeparture.click();
            case 'arrival':
                return this.sortByArrival.click();
            case 'duration':
                return this.sortByDuration.click();
        }
    }

    async changeSortDirection(): Promise<void> {
        await this.setSortBy(this.type);
    }
}
