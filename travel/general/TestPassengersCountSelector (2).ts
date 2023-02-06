import {Component} from 'components/Component';
import {TestSelect} from 'components/TestSelect';

export class TestPassengersCountSelector extends Component {
    hint: Component;
    select: TestSelect;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.select = new TestSelect(browser, {
            parent: this.qa,
            current: 'select',
        });

        this.hint = new Component(browser, {parent: this.qa, current: 'hint'});
    }
}
