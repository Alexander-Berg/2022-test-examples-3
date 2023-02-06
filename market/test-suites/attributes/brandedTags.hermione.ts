import 'hermione';
import {expect} from 'chai';

import {login, execScript} from '../../helpers';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import AttributePopup from '../../page-objects/attributePopup';

const PAGE_URL = '/entity/ticket@110833294/edit';
const BERU_TAG_SELECTOR = '//li/div/span[contains(.,"ocrm-1471 Покупки")]';
const NON_BERU_TAG_SELECTOR = '//li/div/span[contains(.,"ocrm-1471 не Покупки")]';

const CLEAR_TAGS_SCRIPT = `
    api.bcp.edit('ticket@110833294',
    ['tags': []])
`;

/**
 * Проверяем, что:
 * В обращении для выбора доступны тэги, связанные с брэндом обращения
 * и не доступны тэги из других брендов.
 */
describe(`ocrm-1471: В обращениях тэги фильтруются по бренду`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`При выборе тэгов доступны только тэги, связанные с брендом обращения`, async function() {
        const tagsInput = new ContentWithLabel(this.browser, 'body', '[data-ow-test-attribute-container="tags"]');

        const tagsPopup = new AttributePopup(this.browser, 'body', '[data-ow-test-attribute-popup="tags"]');

        await execScript(this.browser, CLEAR_TAGS_SCRIPT);

        this.browser.refresh();
        await tagsInput.isDisplayed();

        await tagsInput.click();

        await tagsPopup.isDisplayed();

        const brandedTagIsDisplayed = (await this.browser.$$(BERU_TAG_SELECTOR)).length === 1;
        const nonBeruTagIsNotDisplayed = (await this.browser.$$(NON_BERU_TAG_SELECTOR)).length === 0;

        expect(brandedTagIsDisplayed).to.equal(true, 'Тэг, связанный с брендом, не отображается в списке');
        expect(nonBeruTagIsNotDisplayed).to.equal(true, 'Тэг, связанный с другим брендом, отображается в списке');
    });
});
