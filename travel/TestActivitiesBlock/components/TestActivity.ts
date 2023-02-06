import TestTripImage from 'helpers/project/trips/components/TestTripImage';

import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';

export default class TestActivity extends Component {
    title: Component;
    image: TestTripImage;
    link: TestLink;
    label: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.label = new Component(this.browser, {
            parent: this.qa,
            current: 'label',
        });
        this.image = new TestTripImage(this.browser, {
            parent: this.qa,
            current: 'image',
        });
        this.link = new TestLink(this.browser, {
            parent: this.qa,
            current: 'link',
        });
    }

    async hasCorrectLink(): Promise<boolean> {
        const linkUrl = await this.link.getUrl();

        if (!linkUrl) {
            return false;
        }

        return ['https://izi.travel', 'https://afisha.tst.yandex.ru'].includes(
            linkUrl.origin,
        );
    }
}
