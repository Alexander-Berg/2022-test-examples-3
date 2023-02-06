import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {ComponentArray} from 'components/ComponentArray';
import TestPriceItem from './components/TestPriceItem';
import TestBottomSheet from 'components/TestBottomSheet';

export default class TestPriceExplanation extends Component {
    questionIcon: Button;
    priceItems: ComponentArray<TestPriceItem>;

    private bottomSheet: TestBottomSheet;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.questionIcon = new Button(browser, {
            parent: this.qa,
            current: 'questionIcon',
        });

        this.priceItems = new ComponentArray<TestPriceItem>(
            browser,
            {parent: this.qa, current: 'priceItem'},
            TestPriceItem,
        );

        this.bottomSheet = new TestBottomSheet(browser);
    }

    async close(): Promise<void> {
        if (this.isTouch) {
            await this.bottomSheet.close();
        }
    }

    async open(): Promise<void> {
        await this.questionIcon.click();
    }
}
