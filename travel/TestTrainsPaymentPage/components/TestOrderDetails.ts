import extractNumber from 'helpers/utilities/extractNumber';

import {Component} from 'components/Component';

export default class TestOrderDetails extends Component {
    directions: Component;
    additional: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.directions = new Component(browser, {
            parent: this.qa,
            current: 'directions',
        });
        this.additional = new Component(browser, {
            parent: this.qa,
            current: 'additional',
        });
    }

    async extractCoachNumber(): Promise<number> {
        const coachText = await this.findAdditionalItem('вагон');
        const coachNumber = coachText ? extractNumber(coachText) : undefined;

        if (!coachNumber) {
            throw new Error('Не найден номер вагона');
        }

        return coachNumber;
    }

    async extractPlaceNumber(): Promise<number> {
        const coachText = await this.findAdditionalItem('место');
        const placeNumber = coachText ? extractNumber(coachText) : undefined;

        if (!placeNumber) {
            throw new Error('Не найден номер вагона');
        }

        return placeNumber;
    }

    private async findAdditionalItem(
        text: string,
    ): Promise<string | undefined> {
        const additionalText = await this.additional.getText();

        return additionalText
            .split(',')
            .map(s => s.trim())
            .find(s => s.includes(text));
    }
}
