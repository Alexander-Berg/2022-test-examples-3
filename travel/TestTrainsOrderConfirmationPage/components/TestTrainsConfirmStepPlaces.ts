import {TestPassengers} from 'helpers/project/trains/pages/TestTrainsOrderConfirmationPage/components/TestPassengers/TestPassengers';
import extractNumbers from 'helpers/utilities/extractNumbers';
import extractNumber from 'helpers/utilities/extractNumber';
import TestTrainCoachTransportSchema from 'helpers/project/trains/components/TestTrainCoach/components/TestTrainCoachTransportSchema';

import {Component} from 'components/Component';

export class TestTrainsConfirmStepPlaces extends Component {
    schemaWrapper: Component;
    transportSchema: TestTrainCoachTransportSchema;
    coachNumber: Component;
    places: Component;
    direction: Component;
    passengers: Component;
    serviceClass: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.direction = new Component(browser, {
            parent: this.qa,
            current: 'direction',
        });
        this.schemaWrapper = new Component(browser, {
            parent: this.qa,
            current: 'schemaWrapper',
        });
        this.transportSchema = new TestTrainCoachTransportSchema(browser, {
            parent: this.qa,
            current: 'transportSchema',
        });
        this.coachNumber = new Component(browser, {
            parent: this.qa,
            current: 'coachNumber',
        });
        this.serviceClass = new Component(browser, {
            parent: this.qa,
            current: 'serviceClass',
        });
        this.places = new Component(browser, {
            parent: this.qa,
            current: 'places',
        });
        this.passengers = new TestPassengers(browser, {
            parent: this.qa,
            current: 'passengers',
        });
    }

    async getCoachNumber(): Promise<number | undefined> {
        const number = await this.coachNumber.getText();

        return extractNumber(number);
    }

    async getServiceClassCode(): Promise<string | undefined> {
        const classTitle = await this.serviceClass.getText();

        return classTitle.match(/\s+(\d[А-ЯЁ])/)?.[1];
    }

    async getPlaces(): Promise<number[] | undefined> {
        const places = await this.places.getText();

        return extractNumbers(places);
    }
}
