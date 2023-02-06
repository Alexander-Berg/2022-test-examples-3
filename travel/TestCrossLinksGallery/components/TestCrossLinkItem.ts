import {Component} from 'components/Component';
import TestTravelImage from 'components/TestTravelImage';
import {TestPrice} from 'components/TestPrice';
import {TestLink} from 'components/TestLink';

export default class TestCrossLinkItem extends Component {
    image: TestTravelImage;
    title: Component;
    price: TestPrice;
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

        this.price = new TestPrice(this.browser, {
            parent: this.qa,
            current: 'price',
        });

        this.link = new TestLink(this.browser, {
            parent: this.qa,
            current: 'link',
        });
    }

    async getFrom(): Promise<string> {
        const parts = await this.parseFromTo();

        return parts[0];
    }

    async getTo(): Promise<string> {
        const parts = await this.parseFromTo();

        return parts[1];
    }

    private async parseFromTo(): Promise<string[]> {
        const route = await this.title.getText();

        return route.split('—').map(s => s.trim());
    }
}
