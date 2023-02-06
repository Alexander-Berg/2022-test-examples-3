import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import {TestCoachTypeGroupItem} from './TestCoachTypeGroupItem';

export class TestCoachTypeGroup extends Component {
    title: Component;
    classes: ComponentArray<TestCoachTypeGroupItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.classes = new ComponentArray(
            browser,
            {parent: this.qa, current: 'coachTypeGroupItem'},
            TestCoachTypeGroupItem,
        );
    }
}
