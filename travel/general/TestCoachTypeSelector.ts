import {filter} from 'p-iteration';

import {Radio, Component} from '../../common/components';

export class TestCoachTypeSelector extends Component {
    platzkarteButton: Radio;
    compartmentButton: Radio;
    suiteButton: Radio;
    buttons: Radio[];

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.platzkarteButton = new Radio(browser, {
            parent: this.qa,
            current: 'button',
            key: 'platzkarte',
        });

        this.compartmentButton = new Radio(browser, {
            parent: this.qa,
            current: 'button',
            key: 'compartment',
        });

        this.suiteButton = new Radio(browser, {
            parent: this.qa,
            current: 'button',
            key: 'suite',
        });

        this.buttons = [
            this.platzkarteButton,
            this.compartmentButton,
            this.suiteButton,
        ];
    }

    get visibleButtons() {
        return filter(this.buttons, async button => await button.isDisplayed());
    }

    async getActive() {
        return this.findByActiveAttr('true');
    }

    async getInctive() {
        return this.findByActiveAttr('false');
    }

    private async findByActiveAttr(value: string) {
        for (let i = 0; i < this.buttons.length; i++) {
            try {
                const active = await this.buttons[i].getAttribute(
                    'data-active',
                );

                if (active === value) {
                    return this.buttons[i];
                }
            } catch (e) {}
        }

        const searchFor = value === 'false' ? 'неактивных' : 'активных';

        throw new Error(`Нет ${searchFor} кнопок в компоненте "${this.qa}"`);
    }
}
