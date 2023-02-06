import {Component} from 'components/Component';

export default class TestAviaPartnerLogo extends Component {
    readonly img: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.img = new Component(browser, {
            parent: this.qa,
            current: 'img',
        });
    }

    getSrc(): Promise<string | null> {
        return this.img.getAttribute('src');
    }
}
