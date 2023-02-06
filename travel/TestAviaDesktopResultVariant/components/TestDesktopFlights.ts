import IFlightInfo from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/types/IFlightInfo';

import {TestLogosWithTitle} from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaDesktopResultVariant/components/TestLogosWithTitle';

import {Component} from 'components/Component';

export default class TestDesktopFlights extends Component {
    departureTime: Component;
    duration: Component;
    arrivalTime: Component;
    departure: Component;
    arrival: Component;
    airline: TestLogosWithTitle;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.airline = new TestLogosWithTitle(browser, {
            parent: this.qa,
            current: 'airline',
        });
        this.departureTime = new Component(browser, {
            parent: this.qa,
            current: 'departureTime',
        });
        this.arrivalTime = new Component(browser, {
            parent: this.qa,
            current: 'arrivalTime',
        });
        this.departure = new Component(browser, {
            parent: this.qa,
            current: 'departure',
        });
        this.arrival = new Component(browser, {
            parent: this.qa,
            current: 'arrival',
        });
        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });
    }

    async getFlightInfo(): Promise<IFlightInfo> {
        const departureIATA = (await this.departure.getText()).split('\n');
        const arrivalIATA = (await this.arrival.getText()).split('\n');

        return {
            departureTime: await this.departureTime.getText(),
            arrivalTime: await this.arrivalTime.getText(),
            duration: await this.duration.getText(),
            arrival: await this.arrival.getText(),
            departure: await this.departure.getText(),
            departureIATA: departureIATA[1],
            arrivalIATA: arrivalIATA[1],
        };
    }
}
