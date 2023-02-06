/* eslint-disable no-await-in-loop */

import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import Button from '../../page-objects/button';
import Autocomplete from '../../page-objects/autocomplete';
import Toast from '../../page-objects/toast';
import {LONG_TIMEOUT_MS} from '../../constants';

const PAGE_URL = '/entity/catalog@serviceRule';
const SERVICE_TITLE = 'Очередь с брендом Покупки';
const CHANNEL_TITLE = 'YouScan';

/**
 * Проверяем, что при настройке роутинга очереди в модальном окне добавления
 * нового элемента при выборе категорий отображаются бренды, к которым они относятся;
 * сохранение элемента происходит успешно.
 */
describe('Над категориями отображается бренд, если можно выбирать категории разных брендов', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Проверяет выбор категории по бренду и добавляет новый элемент настройки роутинга очереди', async function() {
        hermione.skip.in(['yandex-browser', 'chrome'], 'После выполнения OCRM-10278 убрать skip');

        const createButton = new Button(this.browser, 'body', '[data-ow-test-toolbar-create-action="create"]');

        await createButton.clickButton();
        const categoryInput = await this.browser.$('[data-ow-test-properties-list-attribute="category"] input');

        const categoryInputIsDisplayed = await categoryInput.waitForDisplayed({timeout: LONG_TIMEOUT_MS});

        expect(categoryInputIsDisplayed).to.equal(true, 'Не открылось модальное окно добавления элемента');

        await categoryInput.click();
        await categoryInput.addValue('test sanity');
        const popupCategories = await this.browser.$('[data-ow-test-popup="category"]');

        await popupCategories.waitForExist();
        const pokupkiCategory = await this.browser.$('span=Покупки');

        await pokupkiCategory.waitForDisplayed({timeout: LONG_TIMEOUT_MS});
        const pokupkiCategoryCategoryParent = await (
            await (await (await pokupkiCategory.parentElement()).parentElement()).parentElement()
        ).parentElement();

        await pokupkiCategoryCategoryParent.isDisplayed();
        const testSanityCategory = await pokupkiCategoryCategoryParent.$('span=test sanity');

        const testSanityCategoryIsDisplayed = await testSanityCategory.waitForDisplayed({timeout: LONG_TIMEOUT_MS});

        expect(testSanityCategoryIsDisplayed).to.equal(true, 'Нужной категории нет в списке');

        const testSanityCategoryParent = await (
            await (await testSanityCategory.parentElement()).parentElement()
        ).parentElement();

        const checkbox = testSanityCategoryParent.$('[data-ow-test-checkbox] div');

        await checkbox.isDisplayed();
        await checkbox.click();

        const serviceInput = new Autocomplete(
            this.browser,
            'body',
            '[data-ow-test-properties-list-attribute="service"]'
        );

        await serviceInput.selectSingleItem(SERVICE_TITLE);

        const channelInput = new Autocomplete(
            this.browser,
            'body',
            '[data-ow-test-properties-list-attribute="channel"]'
        );

        await channelInput.selectSingleItem(CHANNEL_TITLE);
        const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');

        await saveButton.isEnabled();
        await saveButton.clickButton();

        const successToast = new Toast(this.browser, 'body', '[data-ow-test-toast="success"]');

        const successToastIsDisplayed = await successToast.isDisplayed();

        expect(successToastIsDisplayed).to.equal(true, 'Не появилось уведомление об успешном сохранении объекта');
    });
});
