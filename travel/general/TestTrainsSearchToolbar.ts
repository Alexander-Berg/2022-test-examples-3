import {TestTrainsSorting} from 'helpers/project/trains/components/TestTrainsSorting';

import {Component} from 'components/Component';

export class TestTrainsSearchToolbar extends Component {
    sorting: TestTrainsSorting;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.sorting = new TestTrainsSorting(browser, {
            parent: this.qa,
            current: 'sorting',
        });
    }
}
