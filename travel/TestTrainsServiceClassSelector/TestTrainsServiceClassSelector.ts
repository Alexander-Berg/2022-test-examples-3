import {Component} from 'components/Component';
import {Button} from 'components/Button';

import {TestCoachTypeGroupItem} from '../TestCoachTypeGroupItem';
import {TestTrainsServiceClassSelectorModal} from './TestTrainsServiceClassSelectorModal';

export class TestTrainsServiceClassSelector extends Component {
    coachTypeGroup: TestCoachTypeGroupItem;
    selectClassButton: Button;
    modal: TestTrainsServiceClassSelectorModal;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coachTypeGroup = new TestCoachTypeGroupItem(browser, {
            parent: this.qa,
            current: 'coachTypeGroupItem',
        });

        this.selectClassButton = new Button(browser, {
            parent: this.qa,
            current: 'selectClassButton',
        });

        this.modal = new TestTrainsServiceClassSelectorModal(browser, {
            parent: this.qa,
            current: 'modal',
        });
    }
}
