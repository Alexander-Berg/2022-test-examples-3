import {TestTrainOrderSegments} from 'helpers/project/trains/components/TestTrainOrderSegments/TestTrainOrderSegments';

import {Component} from 'components/Component';

export default class TestSegments extends TestTrainOrderSegments {
    partnerReservationNumbers: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.partnerReservationNumbers = new Component(browser, {
            parent: this.qa,
            current: 'partnerReservationNumbers',
        });
    }

    async getPartnerReservationNumbers(): Promise<number[]> {
        const numbers = await this.partnerReservationNumbers.getText();

        return numbers.match(/\d+/g)?.map(Number) ?? [];
    }
}
