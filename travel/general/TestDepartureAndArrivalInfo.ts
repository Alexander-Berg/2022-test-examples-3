import {Component} from 'components/Component';

export default class TestDepartureAndArrivalInfo extends Component {
    fromTimeTopDescription: Component;
    durationTopDescription: Component;
    toTimeTopDescription: Component;
    fromTime: Component;
    duration: Component;
    toTime: Component;
    fromTimeBottomDescription: Component;
    durationBottomDescription: Component;
    toTimeBottomDescription: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.fromTimeTopDescription = new Component(browser, {
            parent: this.qa,
            current: 'fromTimeTopDescription',
        });
        this.durationTopDescription = new Component(browser, {
            parent: this.qa,
            current: 'durationTopDescription',
        });
        this.toTimeTopDescription = new Component(browser, {
            parent: this.qa,
            current: 'toTimeTopDescription',
        });
        this.fromTime = new Component(browser, {
            parent: this.qa,
            current: 'fromTime',
        });
        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });
        this.toTime = new Component(browser, {
            parent: this.qa,
            current: 'toTime',
        });
        this.fromTimeBottomDescription = new Component(browser, {
            parent: this.qa,
            current: 'fromTimeBottomDescription',
        });
        this.durationBottomDescription = new Component(browser, {
            parent: this.qa,
            current: 'durationBottomDescription',
        });
        this.toTimeBottomDescription = new Component(browser, {
            parent: this.qa,
            current: 'toTimeBottomDescription',
        });
    }
}
