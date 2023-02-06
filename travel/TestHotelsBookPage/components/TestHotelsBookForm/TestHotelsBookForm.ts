import _flatten from 'lodash/flatten';

import {SECOND} from 'helpers/constants/dates';

import {ITestFormDocument} from 'components/TestBookingPassengerForm/types';
import {ITestFormContacts} from 'components/TestBookingContactsForm/types';

import {Form} from 'components/Form';
import {ComponentArray} from 'components/ComponentArray';
import {TestBookingContactsForm} from 'components/TestBookingContactsForm/TestBookingContactsForm';
import {Component} from 'components/Component';
import {TestBookingPassengerForm} from 'components/TestBookingPassengerForm/TestBookingPassengerForm';

export class TestHotelsBookForm extends Form {
    readonly contacts: TestBookingContactsForm;
    readonly guests: ComponentArray<TestBookingPassengerForm>;
    readonly goToForm: Component;

    constructor(
        browser: WebdriverIO.Browser,
        qa: QA,
        submitQa: QA,
        goToFormQa: QA,
    ) {
        super(browser, qa, submitQa);

        this.guests = new ComponentArray(
            browser,
            {parent: this.qa, current: 'guests'},
            TestBookingPassengerForm,
        );

        this.contacts = new TestBookingContactsForm(browser, {
            parent: this.qa,
            current: 'contacts',
        });

        this.goToForm = new Component(browser, goToFormQa);
    }

    async getGuestFields(): Promise<TestBookingPassengerForm[]> {
        return await this.guests.items;
    }

    async checkEmailField(): Promise<boolean> {
        return await this.contacts.email.isVisible();
    }

    async checkPhoneField(): Promise<boolean> {
        return await this.contacts.phone.isVisible();
    }

    async checkFirstAndLastNameFields(): Promise<boolean> {
        const guestFields = await this.getGuestFields();
        const visibleFirstAndLastNameFields = await Promise.all(
            guestFields.map(async guestField =>
                Promise.all([
                    await guestField.firstName.isVisible(),
                    await guestField.lastName.isVisible(),
                ]),
            ),
        );

        return _flatten(visibleFirstAndLastNameFields).every(Boolean);
    }

    async fillForm(
        guests: ITestFormDocument[],
        contacts: ITestFormContacts,
    ): Promise<void> {
        await this.guests.forEach(async (item, index) => {
            const guest = guests[index];

            if (guest) {
                await item.fill(guest, {hasPatronymic: false});
            }
        });

        await this.contacts.fill(contacts);
    }

    async submit(): Promise<void> {
        if (this.isTouch) {
            /**
             * В таче поверх иногда налезает кнопка "Ввести имена гостей" (хотя на скринах ее нет)
             * которая должна исчезать при скролле, но почему-то вебдрайвер иногда видит клик по ней
             * после своего собственного скролла, поэтому скроллим сами и ждем пока плашка исчезнет
             */
            await this.submitButton.scrollIntoView();

            if (await this.goToForm.isVisible()) {
                await this.goToForm.waitForHidden(3 * SECOND);
            }
        }

        await super.submit();
    }
}
