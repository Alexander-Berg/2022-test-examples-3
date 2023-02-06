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

        const title = await this.title.getText();

        await this.browser.waitUntil(
            async () => {
                await super.click();

                const nextCurrent = await this.isChecked();

                return current !== nextCurrent;
            },
            {
                timeoutMsg: `Не удалось выбрать авиакомпанию "${title}" в фильтре`,
            },
        );
    }
}
