import {TestErrorModal} from 'helpers/project/account/components/TestErrorModal';
import {TestOrderHeader} from 'helpers/project/common/components/TestOrderHeader';

import {Component} from 'components/Component';
import {Loader} from 'components/Loader';
import TestWarnings from './components/TestWarnings';
import TestOrderActions from './components/TestOrderActions';
import TestPassengers from './components/TestPassengers/TestPassengers';
import TestContacts from './components/TestContacts';
import TestSegments from './components/TestSegments';
import TestOrderOrchActionModal from './components/TestOrderOrchActionModal';

export class TestTrainsGenericOrderPage extends Component {
    loader: Loader;
    header: TestOrderHeader;
    segmentsInfo: TestSegments;
    warnings: TestWarnings;
    orderActions: TestOrderActions;
    passengers: TestPassengers;
    contacts: TestContacts;

    orderOrchActionModal: TestOrderOrchActionModal;
    errorModal: TestErrorModal;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.loader = new Loader(browser, 'loader');
        this.header = new TestOrderHeader(browser, {
            parent: this.qa,
            current: 'header',
        });
        this.segmentsInfo = new TestSegments(browser, {
            parent: this.qa,
            current: 'segmentsInfo',
        });
        this.warnings = new TestWarnings(browser, {
            parent: this.qa,
            current: 'warnings',
        });
        this.orderActions = new TestOrderActions(browser, {
            parent: this.qa,
            current: 'orderActions',
        });
        this.passengers = new TestPassengers(browser, {
            parent: this.qa,
            current: 'passengers',
        });
        this.contacts = new TestContacts(browser, {
            parent: this.qa,
            current: 'contacts',
        });

        this.orderOrchActionModal = new TestOrderOrchActionModal(browser);
        this.errorModal = new TestErrorModal(browser);
    }

    waitOrderLoaded(): Promise<void> {
        return this.loader.waitUntilLoaded();
    }
}
