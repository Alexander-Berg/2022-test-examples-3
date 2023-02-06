import {ITestFormDocument} from 'helpers/project/common/components/TestBookingPassengerForm/types';

import {Component} from 'components/Component';
import {TestBookingPassengerForm} from 'components/TestBookingPassengerForm/TestBookingPassengerForm';
import {
    ILoyaltyCardsForm,
    TestLoyaltyCards,
} from '../pages/TestTrainsOrderPassengersStepPage/components/TestLoyaltyCards';

export interface ITrainsTestFormDocument extends ITestFormDocument {
    loyaltyCards?: ILoyaltyCardsForm;
}

export class TestTrainsBookingPassengerForm extends Component {
    readonly passengerForm: TestBookingPassengerForm;
    readonly loyaltyCards: TestLoyaltyCards;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.passengerForm = new TestBookingPassengerForm(browser, {
            parent: this.qa,
            current: 'passengerForm',
        });
        this.loyaltyCards = new TestLoyaltyCards(browser, {
            parent: this.qa,
            current: 'additionalFieldsFooter-loyaltyCards',
        });
    }

    async fill(passengerData: ITrainsTestFormDocument): Promise<void> {
        await this.passengerForm.fill(passengerData, {hasPatronymic: true});

        if (passengerData.loyaltyCards) {
            await this.loyaltyCards.fill(passengerData.loyaltyCards);
        }
    }
}
