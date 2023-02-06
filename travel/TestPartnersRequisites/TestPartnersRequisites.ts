import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {TestModal} from 'components/TestModal';
import TestPartnersInfo from './components/TestPartnersInfo/TestPartnersInfo';

export default class TestPartnersRequisites extends Component {
    readonly button: Button;
    readonly modal: TestModal;
    readonly partnersInfo: TestPartnersInfo;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.button = new Button(browser, {
            parent: this.qa,
            current: 'openLink',
        });

        this.modal = new TestModal(browser, {
            parent: this.qa,
            current: 'modal',
        });

        this.partnersInfo = new TestPartnersInfo(browser, {
            parent: this.qa,
            current: 'partnersInfo',
        });
    }
}
