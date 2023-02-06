import {Component} from 'components/Component';
import {TestHotelsStaticMap} from 'components/TestHotelsStaticMap';
import {TestLink} from 'components/TestLink';

export class TestHotelsCrossSaleMap extends Component {
    readonly map: TestHotelsStaticMap;
    readonly searchLink: TestLink;
    readonly buttonSearchLink: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.map = new TestHotelsStaticMap(this.browser, {
            parent: this.qa,
            current: 'map',
        });
        this.searchLink = new TestLink(this.browser, {
            parent: this.qa,
            current: 'searchLink',
        });
        this.buttonSearchLink = new TestLink(this.browser, {
            parent: this.qa,
            current: 'buttonSearchLink',
        });
    }
}
