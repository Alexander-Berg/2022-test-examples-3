import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestLogo extends Component {
    airlines: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.airlines = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'airline',
            },
            Component,
        );
    }
}
