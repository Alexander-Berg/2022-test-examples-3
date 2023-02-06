import {Component} from 'components/Component';

export default class TestBusesSearchSegmentPoint extends Component {
    date: Component;
    time: Component;
    station: Component;
    unknownArrival: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.date = new Component(browser, {
            parent: this.qa,
            current: 'date',
        });

        this.time = new Component(browser, {
            parent: this.qa,
            current: 'time',
        });

        this.station = new Component(browser, {
            parent: this.qa,
            current: 'station',
        });

        this.unknownArrival = new Component(browser, {
            parent: this.qa,
            current: 'unknownArrival',
        });
    }

    async isDisplayedCorrectly(): Promise<boolean> {
        return (
            (await this.unknownArrival.isDisplayed()) ||
            ((await this.date.isDisplayed()) &&
                (await this.time.isDisplayed()) &&
                (await this.station.isDisplayed()))
        );
    }
}
