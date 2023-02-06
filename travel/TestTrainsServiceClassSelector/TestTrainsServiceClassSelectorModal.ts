import {TestModalWithBackButton} from 'components/TestModalWithBackButton';

import {TestCoachTypeGroups} from '../TestCoachTypeGroups';

export class TestTrainsServiceClassSelectorModal extends TestModalWithBackButton {
    coachTypeGroups: TestCoachTypeGroups;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coachTypeGroups = new TestCoachTypeGroups(browser, {
            parent: this.qa,
            current: 'coachTypeGroup',
        });
    }
}
