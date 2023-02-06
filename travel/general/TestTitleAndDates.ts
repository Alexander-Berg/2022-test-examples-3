import {Component} from 'components/Component';

export default class TestTitleAndDates extends Component {
    title: Component;
    dateForward: Component;
    directionForward: Component;
    dateBackward: Component;
    directionBackward: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.dateForward = new Component(this.browser, {
            parent: this.qa,
            current: 'dateForward',
        });
        this.directionForward = new Component(this.browser, {
            parent: this.qa,
            current: 'directionForward',
        });
        this.dateBackward = new Component(this.browser, {
            parent: this.qa,
            current: 'dateBackward',
        });
        this.directionBackward = new Component(this.browser, {
            parent: this.qa,
            current: 'directionBackward',
        });
    }

    async areDirectionsVisible(): Promise<boolean> {
        return (
            (await this.directionForward.isVisible()) &&
            (await this.directionBackward.isVisible())
        );
    }
}
