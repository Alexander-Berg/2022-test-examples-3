import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestAdvantage from 'components/TestAdvantages/components/TestAdvantage';

export default class TestAdvantages extends Component {
    advantages: ComponentArray<TestAdvantage>;
    title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });

        this.advantages = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'advantage',
            },
            TestAdvantage,
        );
    }
}
