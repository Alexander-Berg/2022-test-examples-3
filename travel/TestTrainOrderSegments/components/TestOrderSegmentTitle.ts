import {Component} from 'components/Component';

export class TestOrderSegmentTitle extends Component {
    direction: Component;
    departure: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.direction = new Component(browser, {
            parent: this.qa,
            current: 'direction',
        });

        this.departure = new Component(browser, {
            parent: this.qa,
            current: 'departure',
        });
    }

    async getDepartureAndArrival(): Promise<[string, string]> {
        const direction = await this.direction.getText();

        return direction.split(' — ') as [string, string];
    }

    async getDepartureDate(): Promise<string> {
        return this.departure.getText();
    }
}
