import {Component} from 'components/Component';
import TestTravelImage from 'components/TestTravelImage';

export default class TestTripImage extends Component {
    readonly image: TestTravelImage;
    readonly stubContainer: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.image = new TestTravelImage(browser, {
            parent: this.qa,
            current: 'image',
        });

        this.stubContainer = new Component(browser, {
            parent: this.qa,
            current: 'stubContainer',
        });
    }

    async isStub(): Promise<boolean> {
        return this.stubContainer.isVisible();
    }
}
