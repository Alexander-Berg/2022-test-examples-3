import {Component} from 'components/Component';

export class TestTrainsSearchFooter extends Component {
    linkBackward: Component;
    electronicTicketDisclaimer: Component;
    partnerInfoDisclaimer: Component;
    dynamicPricing: Component | undefined;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'trainsSearchFooter') {
        super(browser, qa);

        this.linkBackward = new Component(browser, {
            parent: this.qa,
            current: 'linkBackward',
        });
        this.electronicTicketDisclaimer = new Component(browser, {
            parent: this.qa,
            current: 'electronicTicketDisclaimer',
        });
        this.partnerInfoDisclaimer = new Component(browser, {
            parent: this.qa,
            current: 'partnerInfoDisclaimer',
        });

        if (this.isTouch) {
            this.dynamicPricing = new Component(browser, {
                parent: this.qa,
                current: 'dynamicPricing',
            });
        }
    }
}
