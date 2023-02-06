import {Component} from 'components/Component';
import {TestBeddingOption} from './components/TestBeddingOption';

export class TestBedding extends Component {
    beddingIncluded: Component;
    option: TestBeddingOption;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.beddingIncluded = new Component(browser, {
            parent: this.qa,
            current: 'included',
        });

        this.option = new TestBeddingOption(browser, {
            parent: this.qa,
            current: 'option',
        });
    }

    async isChecked(): Promise<boolean> {
        return (
            (await this.beddingIncluded.isDisplayed()) ||
            (await this.option.checkbox.isChecked())
        );
    }

    async getPrice(): Promise<number | null> {
        try {
            return await this.option.price.getPriceValue();
        } catch (err) {
            return null;
        }
    }
}
