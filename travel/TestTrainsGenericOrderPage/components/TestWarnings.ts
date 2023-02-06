import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestWarnings extends Component {
    warnings: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.warnings = new ComponentArray(
            browser,
            {parent: this.qa, current: 'warning'},
            Component,
        );
    }
}
