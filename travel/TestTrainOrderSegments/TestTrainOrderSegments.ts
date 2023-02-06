import {TestTrainsOrderSegment} from 'helpers/project/trains/components/TestTrainsOrderSegment';
import {TestOrderSegmentTitle} from 'helpers/project/trains/components/TestTrainOrderSegments/components/TestOrderSegmentTitle';
import {TestTransferSeparator} from 'helpers/project/trains/components/TestTrainOrderSegments/components/TestTransferSeparator/TestTransferSeparator';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestTrainOrderSegments extends Component {
    segments: ComponentArray<TestTrainsOrderSegment>;
    title: TestOrderSegmentTitle;
    transfer: TestTransferSeparator;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'trainsOrderSegments') {
        super(browser, qa);

        this.title = new TestOrderSegmentTitle(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.segments = new ComponentArray(
            browser,
            {parent: this.qa, current: 'trainsOrderSegment'},
            TestTrainsOrderSegment,
        );

        this.transfer = new TestTransferSeparator(browser, {
            parent: this.qa,
            current: 'transfer',
        });
    }

    async getSegment(index: number = 0): Promise<TestTrainsOrderSegment> {
        return await this.segments.at(index);
    }
}
