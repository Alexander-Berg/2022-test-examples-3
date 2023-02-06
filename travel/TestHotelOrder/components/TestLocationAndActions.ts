import TestMapModal from 'helpers/project/account/pages/TripPage/components/TestHotelOrder/components/TestMapModal';

import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestLocationAndActions extends Component {
    address: Component;
    locationButton: Button;
    printButton: Button;
    mapModal: TestMapModal;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.address = new Component(this.browser, {
            parent: this.qa,
            current: 'address',
        });
        this.locationButton = new Component(this.browser, {
            parent: this.qa,
            current: 'locationButton',
        });
        this.printButton = new Component(this.browser, {
            parent: this.qa,
            current: 'printButton',
        });
        this.mapModal = new TestMapModal(this.browser, {
            parent: this.qa,
            current: 'mapModal',
        });
    }
}
