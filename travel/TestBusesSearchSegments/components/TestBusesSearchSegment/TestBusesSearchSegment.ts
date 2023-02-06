import {every} from 'p-iteration';

import TestBusesSearchSegmentPoint from 'helpers/project/buses/components/TestBusesSearchSegments/components/TestBusesSearchSegment/components/TestBusesSearchSegmentPoint';

import {Component} from 'components/Component';

export default class TestBusesSearchSegment extends Component {
    departure: TestBusesSearchSegmentPoint;
    arrival: TestBusesSearchSegmentPoint;
    duration: Component;
    carrier: Component;
    price: Component;
    places: Component;
    actionButton: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.departure = new TestBusesSearchSegmentPoint(browser, {
            parent: this.qa,
            current: 'departure',
        });

        this.arrival = new TestBusesSearchSegmentPoint(browser, {
            parent: this.qa,
            current: 'arrival',
        });

        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });

        this.carrier = new Component(browser, {
            parent: this.qa,
            current: 'carrier',
        });

        this.price = new Component(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.places = new Component(browser, {
            parent: this.qa,
            current: 'info',
        });

        this.actionButton = new Component(browser, {
            parent: this.qa,
            current: 'actionButton',
        });
    }

    async isDisplayedCorrectly(): Promise<boolean> {
        const isPlacesDisplayed = await this.places.isDisplayed();
        const placesText = await this.places.getText();
        const actionButtonText = await this.actionButton.getText();

        return (
            (await this.departure.isDisplayedCorrectly()) &&
            (await this.arrival.isDisplayedCorrectly()) &&
            ((await this.arrival.unknownArrival.isDisplayed()) ||
                (await this.duration.isDisplayed())) &&
            (await every([this.carrier, this.price], async elem =>
                elem.isDisplayed(),
            )) &&
            (!isPlacesDisplayed || /^\d+ мест[оа]?$/.test(placesText)) &&
            actionButtonText === 'Выбрать'
        );
    }

    async isDurationDisplayedCorrectly(): Promise<boolean> {
        return (
            (await this.duration.isDisplayed()) ||
            (await this.arrival.unknownArrival.isDisplayed())
        );
    }
}
