import {Component} from 'components/Component';
import TestTravelImage from 'components/TestTravelImage';
import {TestLink} from 'components/TestLink';

export default class TestForecastItem extends Component {
    image: TestTravelImage;
    title: Component;
    description: Component;
    link: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.image = new TestTravelImage(this.browser, {
            parent: this.qa,
            current: 'image',
        });
        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.description = new Component(this.browser, {
            parent: this.qa,
            current: 'description',
        });
        this.link = new TestLink(this.browser, {
            parent: this.qa,
            current: 'link',
        });
    }
}
