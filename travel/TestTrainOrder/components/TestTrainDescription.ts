import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestTrainDescription extends Component {
    combinedDescription: Component;
    transferText: Component;
    trainDescriptions: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.combinedDescription = new Component(this.browser, {
            parent: this.qa,
            current: 'combinedDescription',
        });
        this.transferText = new Component(this.browser, {
            parent: this.qa,
            current: 'transferText',
        });
        this.trainDescriptions = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'trainDescription',
            },
            Component,
        );
    }
}
