import TestTrainDescription from 'helpers/project/account/pages/TripPage/components/TestTrainOrder/components/TestTrainDescription';

import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {TestLink} from 'components/TestLink';

export default class TestDescriptionAndActions extends Component {
    trainDescription: TestTrainDescription;
    downloadButton: TestLink;
    printButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.trainDescription = new TestTrainDescription(this.browser, {
            parent: this.qa,
            current: 'trainDescription',
        });
        this.downloadButton = new TestLink(this.browser, {
            parent: this.qa,
            current: 'downloadButton',
        });
        this.printButton = new Button(this.browser, {
            parent: this.qa,
            current: 'printButton',
        });
    }
}
