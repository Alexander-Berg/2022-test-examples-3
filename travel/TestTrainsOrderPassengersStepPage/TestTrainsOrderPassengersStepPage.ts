import TrainsOrderPageLayout from 'helpers/project/trains/components/TrainsOrderPageLayout';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

import {ComponentArray} from 'components/ComponentArray';
import {TestBookingContactsForm} from 'components/TestBookingContactsForm/TestBookingContactsForm';
import {
    ITrainsTestFormDocument,
    TestTrainsBookingPassengerForm,
} from '../../components/TestTrainsBookingPassengerForm';
import {Component} from 'components/Component';

export class TestTrainsOrderPassengersStepPage extends Component {
    readonly contacts: TestBookingContactsForm;
    readonly passengers: ComponentArray<TestTrainsBookingPassengerForm>;
    readonly layout: TrainsOrderPageLayout;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'passengersStep') {
        super(browser, qa);

        this.layout = new TrainsOrderPageLayout(this.browser);

        this.contacts = new TestBookingContactsForm(this.browser, {
            parent: this.qa,
            current: 'contacts',
        });
        this.passengers = new ComponentArray(
            this.browser,
            {parent: this.qa, current: 'passengerSection'},
            TestTrainsBookingPassengerForm,
        );
    }

    async fillPassengers(
        passengers: ITrainsTestFormDocument[] = [PASSENGER],
    ): Promise<void> {
        const passengersBlocks = await this.passengers.items;

        for (let i = 0; i < passengers.length; i++) {
            const passengerData = passengers[i];
            const passengerBlock = passengersBlocks[i];

            await passengerBlock.fill(passengerData);
        }
    }

    async fillContacts(
        contacts: {
            email: string;
            phone: string;
        } = CONTACTS,
    ): Promise<void> {
        await this.contacts.fill(contacts);
    }
}
