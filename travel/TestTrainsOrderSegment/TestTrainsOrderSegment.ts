import {TestTrainsOrderSegmentStations} from 'helpers/project/trains/components/TestTrainsOrderSegment/TestTrainsOrderSegmentStations/TestTrainsOrderSegmentStations';

import {Component} from 'components/Component';

import {TestTrainsOrderSegmentTimeAndDuration} from '../TestTrainsOrderSegmentTimeAndDuration';

export interface ITestOrderSegmentInfo {
    number: string;
    direction: string;
    hasElectronicRegistration: boolean;
    firm: string;
    company: string;
    departureDate: string | undefined;
    departureTime: string;
    departureCity: string;
    departureStation: string;
    arrivalDate: string | undefined;
    arrivalTime: string;
    arrivalStation: string;
    arrivalCity: string;
    duration: string;
    timeMessage: string;
}

export class TestTrainsOrderSegment extends Component {
    numberAndDirection: Component;
    firm: Component;
    electronicRegistration: Component;
    company: Component;
    timeAndDuration: TestTrainsOrderSegmentTimeAndDuration;
    stations: TestTrainsOrderSegmentStations;
    car: Component;
    carType: Component;
    places: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.numberAndDirection = new Component(browser, {
            parent: this.qa,
            current: 'numberAndDirection',
        });

        this.firm = new Component(browser, {
            parent: this.qa,
            current: 'firm',
        });

        this.electronicRegistration = new Component(browser, {
            parent: this.qa,
            current: 'electronicRegistration',
        });

        this.company = new Component(browser, {
            parent: this.qa,
            current: 'company',
        });

        this.timeAndDuration = new TestTrainsOrderSegmentTimeAndDuration(
            browser,
            {
                parent: this.qa,
                current: 'timeAndDuration',
            },
        );

        this.stations = new TestTrainsOrderSegmentStations(browser, {
            parent: this.qa,
            current: 'stations',
        });

        this.car = new Component(browser, {
            parent: this.qa,
            current: 'car',
        });

        this.carType = new Component(browser, {
            parent: this.qa,
            current: 'carType',
        });

        this.places = new Component(browser, {
            parent: this.qa,
            current: 'places',
        });
    }

    async getNumber(): Promise<string> {
        const numberAndDirection = await this.numberAndDirection.getText();

        return numberAndDirection.split(/,?\s/)[1];
    }

    async getDirection(): Promise<string> {
        const numberAndDirection = await this.numberAndDirection.getText();

        return numberAndDirection.split(',')[1].trim();
    }

    async getFirm(): Promise<string> {
        try {
            return await this.firm.getText();
        } catch (err) {
            return '';
        }
    }

    async hasElectronicRegistration(): Promise<boolean> {
        try {
            return Boolean(await this.electronicRegistration.getText());
        } catch (err) {
            return false;
        }
    }

    async getCompany(): Promise<string> {
        try {
            return await this.company.getText();
        } catch (err) {
            return '';
        }
    }

    async getDepartureCity(): Promise<string> {
        try {
            return await this.stations.departure.city.getText();
        } catch (err) {
            return '';
        }
    }

    async getArrivalCity(): Promise<string> {
        try {
            return await this.stations.arrival.city.getText();
        } catch (err) {
            return '';
        }
    }

    async getInfo(): Promise<ITestOrderSegmentInfo> {
        return {
            number: await this.getNumber(),
            direction: await this.getDirection(),
            hasElectronicRegistration: await this.hasElectronicRegistration(),
            firm: await this.getFirm(),
            company: await this.getCompany(),
            departureDate: this.extractDate(
                await this.timeAndDuration.departure.date.getText(),
            ),
            departureTime: await this.timeAndDuration.departure.time.getText(),
            departureStation: await this.stations.departure.station.getText(),
            departureCity: await this.getDepartureCity(),
            arrivalDate: this.extractDate(
                await this.timeAndDuration.arrival.date.getText(),
            ),
            arrivalTime: await this.timeAndDuration.arrival.time.getText(),
            arrivalStation: await this.stations.arrival.station.getText(),
            arrivalCity: await this.getArrivalCity(),
            duration: await this.timeAndDuration.duration.getText(),
            timeMessage: await this.timeAndDuration.timeMessage.getText(),
        };
    }

    async getPlaces(): Promise<number[]> {
        const placesWithLabel = await this.places.getText();

        return placesWithLabel.match(/\d+/g)?.map(Number) ?? [];
    }

    /**
     * Достаем из дат сл. день, 23 мая, понедельник
     * дату 23 мая
     */
    private extractDate(date: string): string | undefined {
        return date.split(',').find(str => /\d+/.test(str));
    }
}
