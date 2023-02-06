import {ETestGender} from 'components/TestBookingPassengerForm/types';

import {Component} from 'components/Component';

export default class TestPassengerInfo extends Component {
    private static extractValue(labelWithValue: string): string {
        const [, value] = labelWithValue.split(':');

        return value.trim();
    }

    name: Component;
    document: Component;
    birthDate: Component;
    gender: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.name = new Component(browser, {parent: this.qa, current: 'name'});
        this.document = new Component(browser, {
            parent: this.qa,
            current: 'document',
        });
        this.birthDate = new Component(browser, {
            parent: this.qa,
            current: 'birthDate',
        });
        this.gender = new Component(browser, {
            parent: this.qa,
            current: 'gender',
        });
    }

    async getDocumentNumber(): Promise<string> {
        const documentWithLabel = await this.document.getText();

        const documentValue = TestPassengerInfo.extractValue(documentWithLabel);

        return documentValue.replace(/[\s-№]+/g, '');
    }

    async getBirthDate(): Promise<string> {
        const birthDateWithLabel = await this.birthDate.getText();

        return TestPassengerInfo.extractValue(birthDateWithLabel);
    }

    async getGender(): Promise<ETestGender> {
        const value = TestPassengerInfo.extractValue(
            await this.gender.getText(),
        );

        if (value === 'мужской') {
            return ETestGender.MALE;
        }

        return ETestGender.FEMALE;
    }

    async getFirstName(): Promise<string> {
        return (await this.getFullName())[1];
    }

    async getLastName(): Promise<string> {
        return (await this.getFullName())[0];
    }

    async getPatronymic(): Promise<string> {
        return (await this.getFullName())[2];
    }

    private async getFullName(): Promise<string[]> {
        return (await this.name.getText()).split(' ');
    }
}
