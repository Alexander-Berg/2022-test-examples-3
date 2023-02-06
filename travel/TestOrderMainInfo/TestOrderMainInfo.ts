import moment, {Moment} from 'moment';

import dateFormats from 'helpers/utilities/date/formats';

import {Component} from 'components/Component';

export default class TestOrderMainInfo extends Component {
    readonly title: Component;
    readonly startDate: Component;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'orderTrainsInfo');

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.startDate = new Component(browser, {
            parent: this.qa,
            current: 'startDate',
        });
    }

    async getStartDate(): Promise<Moment> {
        const startDateText = await this.startDate.getText();
        const [date] = startDateText.split(',');

        return moment(date, dateFormats.HUMAN);
    }
}
