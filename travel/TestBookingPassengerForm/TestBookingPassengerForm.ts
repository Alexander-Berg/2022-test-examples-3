import {ITestFormDocument} from 'components/TestBookingPassengerForm/types';

import {Input} from 'components/Input';
import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {TestSexField} from 'components/TestBookingPassengerForm/SexField';
import {ComponentArray} from 'components/ComponentArray';
import {TestCheckbox} from 'components/TestCheckbox';

export class TestBookingPassengerForm extends Component {
    readonly lastName: Input;
    readonly firstName: Input;
    readonly patronymicName: Input;
    private readonly patronymicCheckbox: TestCheckbox;
    private readonly birthday: Input;
    private readonly sex: TestSexField;
    private readonly documentNumber: Input;
    private readonly documentValidDate: Input;
    private readonly intents: ComponentArray<Button>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.lastName = new Input(browser, {
            current: 'lastName',
            parent: this.qa,
        });

        this.firstName = new Input(browser, {
            current: 'firstName',
            parent: this.qa,
        });

        this.patronymicName = new Input(browser, {
            current: 'patronymicName',
            parent: this.qa,
        });

        this.patronymicCheckbox = new TestCheckbox(browser, {
            current: 'patronymicNameCheckbox',
            parent: this.qa,
        });

        this.birthday = new Input(browser, {
            current: 'birthdate',
            parent: this.qa,
        });

        this.sex = new TestSexField(browser, {
            current: 'sex',
            parent: this.qa,
        });

        this.documentNumber = new Input(browser, {
            current: 'documentNumber',
            parent: this.qa,
        });

        this.documentValidDate = new Input(browser, {
            current: 'firstName',
            parent: this.qa,
        });

        this.intents = new ComponentArray(
            browser,
            {parent: this.qa, current: 'intents-intent'},
            Button,
        );
    }

    async fill(
        passenger: ITestFormDocument,
        params: {hasPatronymic: boolean},
    ): Promise<void> {
        if (passenger.lastName) {
            await this.lastName.type(passenger.lastName);
        }

        if (passenger.firstName) {
            await this.firstName.type(passenger.firstName);
        }

        if (passenger.patronymicName) {
            await this.patronymicName.type(passenger.patronymicName);
        } else if (params.hasPatronymic) {
            await this.patronymicCheckbox.click();
        }

        if (passenger.birthdate) {
            await this.birthday.type(passenger.birthdate, true);
        }

        if (passenger.sex) {
            await this.sex.setValue(passenger.sex);
        }

        if (passenger.documentNumber) {
            await this.documentNumber.type(passenger.documentNumber, true);
        }

        if (passenger.documentValidDate) {
            await this.documentValidDate.type(
                passenger.documentValidDate,
                true,
            );
        }
    }

    async fillWithIntent(): Promise<void> {
        const items = await this.intents.items;

        return items[0].click();
    }
}
