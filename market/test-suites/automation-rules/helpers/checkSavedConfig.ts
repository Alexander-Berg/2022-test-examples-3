import {expect} from 'chai';

import CodeEditor from '../../../page-objects/codeEditor';

export const checkSavedConfig = async (browser: WebdriverIO.Browser, expectedConfig: string): Promise<void> => {
    const config = new CodeEditor(
        browser,
        '[data-ow-test-properties-list-attribute="config"]',
        '[data-ow-test-code-editor="json"]'
    );

    await config.isDisplayed();
    const configText = await config.getValue();

    expect(configText).to.equal(expectedConfig, 'Конфигурация сохраненного правила не совпадает с ожидаемой');
};
