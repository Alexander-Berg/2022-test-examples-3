import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestPartner from './components/TestPartner';

export default class TestPartnersInfo extends Component {
    readonly title: Component;
    readonly subTitle: Component;
    readonly partners: ComponentArray<TestPartner>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.subTitle = new Component(browser, {
            parent: this.qa,
            current: 'subTitle',
        });

        this.partners = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'partner',
            },
            TestPartner,
        );
    }
}
