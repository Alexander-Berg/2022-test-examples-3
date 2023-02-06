import {Component} from 'components/Component';

export class TestPrice extends Component {
    constructor(browser: WebdriverIO.Browser, qa: QA = 'price') {
        super(browser, qa);
    }

    async getValue(): Promise<number> {
        const parts = await this.getParts();

        return parts.value;
    }

    async getCurrency(): Promise<string> {
        const parts = await this.getParts();

        return parts.currency;
    }

    /**
     * Проверка точная ли цена,
     * не содержит приставку "от"
     */
    async isExactValue(): Promise<boolean> {
        const text = await this.getText();

        return !/от/.test(text);
    }

    private async getParts(): Promise<{value: number; currency: string}> {
        const text = await this.getText();

        const matches = text
            .replace(/\s/g, '')
            .match(/^([^\d]*)([\d,.]+)(.*)$/);

        if (!matches) {
            throw new Error('Price contains wrong value');
        }

        return {
            value: Number(matches[2].replace(',', '.')),
            currency: matches[3],
        };
    }
}
