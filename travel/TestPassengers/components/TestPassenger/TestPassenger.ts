import {TestTicket} from 'helpers/project/trains/pages/TestTrainsOrderConfirmationPage/components/TestPassengers/components/TestPassenger/components/TestTicket/TestTicket';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestFieldLabel from 'components/TestFieldLabel';

export class TestPassenger extends Component {
    lastName: TestFieldLabel;
    firstName: TestFieldLabel;
    patronymic: TestFieldLabel;
    documentNumber: TestFieldLabel;
    birthDate: TestFieldLabel;

    tickets: ComponentArray<TestTicket>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.lastName = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'lastName',
        });
        this.firstName = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'firstName',
        });
        this.patronymic = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'patronymic',
        });
        this.documentNumber = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'documentNumber',
        });
        this.birthDate = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'birthDate',
        });

        this.tickets = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'ticket',
            },
            TestTicket,
        );
    }

    async getDocumentNumber(): Promise<string> {
        const documentNumber = await this.documentNumber.value.getText();

        return documentNumber.replace(/[\s-№]+/g, '');
    }
}
