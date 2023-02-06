import {Component} from 'helpers/project/common/components';

export class TestHotelPageGeoInfo extends Component {
    address: Component;
    map: Component;
    mapMarker: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.address = new Component(browser, {
            parent: this.qa,
            current: 'address',
        });
        this.map = new Component(browser, {
            parent: this.qa,
            current: 'map',
        });
        this.mapMarker = new Component(browser, {
            parent: this.qa,
            current: 'mapMarker',
        });
    }
}
