import TestOrderMainInfo from 'helpers/project/account/pages/TripPage/components/TestOrderMainInfo';
import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';
import TestLogo from 'helpers/project/account/pages/TripPage/components/TestAviaOrder/components/TestLogo';
import TestPNR from 'helpers/project/account/pages/TripPage/components/TestAviaOrder/components/TestPNR';

export default class TestAviaOrder extends TestOrder {
    orderMainInfo: TestOrderMainInfo;
    logo: TestLogo;
    pnr: TestPNR;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.orderMainInfo = new TestOrderMainInfo(this.browser, {
            parent: this.qa,
            current: 'orderMainInfo',
        });
        this.logo = new TestLogo(this.browser, {
            parent: this.qa,
            current: 'logo',
        });
        this.pnr = new TestPNR(this.browser, {
            parent: this.qa,
            current: 'pnr',
        });
    }
}
