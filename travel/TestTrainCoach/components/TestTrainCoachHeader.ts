import extractNumber from 'helpers/utilities/extractNumber';

import {Component} from 'components/Component';

export default class TestTrainCoachHeader extends Component {
    coachNumber: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coachNumber = new Component(browser, this.qa);
    }

    async getCoachNumber(): Promise<number | undefined> {
        const str = await this.coachNumber.getText();

        return extractNumber(str);
    }
}
