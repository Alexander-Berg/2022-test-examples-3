import {Component} from 'components/Component';

export default class TestOrderHotelsInfo extends Component {
    roomName: Component;
    mealInfo: Component;
    bedGroups: Component;
    images: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.roomName = new Component(this.browser, {
            parent: this.qa,
            current: 'roomName',
        });
        this.mealInfo = new Component(this.browser, {
            parent: this.qa,
            current: 'mealInfo',
        });
        this.bedGroups = new Component(this.browser, {
            parent: this.qa,
            current: 'bedGroups',
        });
        this.images = new Component(this.browser, {
            parent: this.qa,
            current: 'images',
        });
    }
}
