import {Component} from 'components/Component';

import TestCoachTabHeader from './TestCoachTabHeader';
import {TestTransportSchema} from './TestTransportSchema';

export default class TestCoachTabItem extends Component {
    coachHeader: TestCoachTabHeader;
    transportSchema: TestTransportSchema;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coachHeader = new TestCoachTabHeader(browser, {
            parent: this.qa,
            current: 'coachHeader',
        });

        this.transportSchema = new TestTransportSchema(browser, {
            parent: this.qa,
            current: 'transportSchema',
        });
    }
}
