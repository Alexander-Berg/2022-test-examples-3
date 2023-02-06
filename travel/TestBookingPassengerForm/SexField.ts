import {ETestGender} from 'components/TestBookingPassengerForm/types';

import {Component} from 'components/Component';
import {Radio} from 'components/Radio';

export class TestSexField extends Component {
    private readonly maleButton: Radio;
    private readonly femaleButton: Radio;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.maleButton = new Radio(browser, {
            parent: this.qa,
            key: 'male',
        });

        this.femaleButton = new Radio(browser, {
            parent: this.qa,
            key: 'female',
        });
    }

    async setValue(gender: ETestGender): Promise<void> {
        switch (gender) {
            case ETestGender.FEMALE: {
                await this.femaleButton.click();

                break;
            }
            case ETestGender.MALE: {
                await this.maleButton.click();

                break;
            }
        }
    }
}
