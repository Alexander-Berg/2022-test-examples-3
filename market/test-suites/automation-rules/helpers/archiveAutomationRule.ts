import Button from '../../../page-objects/button';
import {TIMEOUT_MS} from '../../../constants';

export const archiveAutomationRule = async (browser: WebdriverIO.Browser): Promise<void> => {
    const archiveButton = new Button(browser, 'body', '[data-ow-test-jmf-card-toolbar-action="edit-архивировать"]');

    await archiveButton.isDisplayed();
    await archiveButton.clickButton();

    await browser
        .$('[data-ow-test-card-header="default"]')
        .$('span=Архивный')
        .waitForDisplayed({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Не дождались архивирования правила',
        });
};
