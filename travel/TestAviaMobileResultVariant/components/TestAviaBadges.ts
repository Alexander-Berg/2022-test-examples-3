import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestAviaBadges extends Component {
    readonly badges: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.badges = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'badge',
            },
            Component,
        );
    }
}
