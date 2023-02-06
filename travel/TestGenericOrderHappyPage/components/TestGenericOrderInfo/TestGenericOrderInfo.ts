import {TestTrainOrderSegments} from 'helpers/project/trains/components/TestTrainOrderSegments/TestTrainOrderSegments';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestGenericOrderInfo extends Component {
    segments: TestTrainOrderSegments;
    footerDescriptions: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.segments = new TestTrainOrderSegments(this.browser);

        this.footerDescriptions = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'footerDescription',
            },
            Component,
        );
    }
}
