import {TestCheckbox} from 'components/TestCheckbox';
import {Component} from 'components/Component';

export class AviaFilterCompaniesCheckbox extends TestCheckbox {
    readonly title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);
        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
    }

    /**
     * Не нажимается с первого раза в выпадающем окне
     */
    async click(): Promise<void> {
        const current = await this.isChecked();
        let nextCurrent;

        do {
            await super.click();

            nextCurrent = await this.isChecked();
        } while (current === nextCurrent);
    }
}
