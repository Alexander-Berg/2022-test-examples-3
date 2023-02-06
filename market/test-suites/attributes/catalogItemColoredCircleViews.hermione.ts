import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import Button from '../../page-objects/button';
import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {CHECK_INTERVAL, TIMEOUT_MS} from '../../constants';

const PAGE_URL = '/entity/root@1?tabBar=5';
const ORDER_NUMBER = '32768823';
const STATUS_COLOR = '#be1e1eff';
const STATUS_TEXT = 'отменен';

/**
 * Проверяем, что:
 * После выбора презентации ColoredCircle для атрибута "Статус" типа CatalogItem
 * в настройках отображения заказов,
 * атрибут отображается в цветного svg-круга
 */
describe(`ocrm-1563: Отображение coloredCircleView`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`После настройки отображения атрибут "Приоритет" должен быть в виде круга`, async function() {
        const searchInput = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-toolbar="search-input"]');

        const searchButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="search-button"]');

        const attributesButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="table-attributes"]');

        const tabBar = new TabsWrapper(
            this.browser,
            '[data-ow-test-settings-modal="tab-bar-content"]',
            '[data-ow-test-attribute-container="tabsWrapper"]'
        );

        const coloredCirclePresentationStatusRadioButton = new ContentWithLabel(
            this.browser,
            '[data-ow-test-radio-group="presentationOf_status"]',
            '[data-ow-test-radio="coloredCircle"]'
        );

        const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');
        const tableValueStatus = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-row-0="status"]');
        const tableValueOrderNumber = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-table-row-0="orderNumberLink"]'
        );

        await tableValueOrderNumber.isDisplayed();
        await tableValueStatus.isDisplayed();

        await searchInput.isDisplayed();
        await searchInput.setValue(ORDER_NUMBER);

        await searchButton.clickButton();

        await this.browser.waitUntil(async () => (await tableValueOrderNumber.link).isClickable(), {
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
        });
        await tableValueStatus.isDisplayed();

        const orderNumber = await (await tableValueOrderNumber.link).getText();

        expect(orderNumber).to.equal(ORDER_NUMBER, 'Заказ не найден');
        const orderStatus = await (await tableValueStatus.span).getText();

        expect(orderStatus).to.equal(STATUS_TEXT, 'Статус заказа не соответствует тест-кейсу');

        await attributesButton.isDisplayed();
        await attributesButton.clickButton();

        await tabBar.isDisplayed();
        await tabBar.clickTab('Настройки отображения');

        await coloredCirclePresentationStatusRadioButton.isDisplayed();
        await coloredCirclePresentationStatusRadioButton.click();

        await saveButton.isDisplayed();
        await saveButton.clickButton();

        const isSaveButtonInvisible = await saveButton.waitForInvisible();

        expect(isSaveButtonInvisible).to.equal(true, 'Модалка редактирования отображения атрибута не закрылась');

        await tableValueStatus.isDisplayed();
        const statusColor = await (await tableValueStatus.elem('a svg')).getAttribute('color');

        expect(statusColor).to.equal(
            STATUS_COLOR,
            'Отображение атрибута типа CatalogItem не в нужном формате или цвете'
        );
    });
});

/**
 * Проверяем, что:
 * После выбора презентации ColoredCircleWithText для атрибута "Статус" типа CatalogItem
 * в настройках отображения заказов,
 * атрибут отображается в цветного svg-круга с текстом
 */
describe(`ocrm-1562: Отображение coloredCircleViewWithText`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`После настройки отображения атрибут "Приоритет" должен быть в виде круга с текстом`, async function() {
        const searchInput = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-toolbar="search-input"]');

        const searchButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="search-button"]');

        const attributesButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="table-attributes"]');

        const tabBar = new TabsWrapper(
            this.browser,
            '[data-ow-test-settings-modal="tab-bar-content"]',
            '[data-ow-test-attribute-container="tabsWrapper"]'
        );

        const coloredCircleWithTextPresentationStatusRadioButton = new ContentWithLabel(
            this.browser,
            '[data-ow-test-radio-group="presentationOf_status"]',
            '[data-ow-test-radio="coloredCircleWithText"]'
        );

        const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');
        const tableValueStatus = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-row-0="status"]');
        const tableValueOrderNumber = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-table-row-0="orderNumberLink"]'
        );

        await tableValueOrderNumber.isDisplayed();
        await tableValueStatus.isDisplayed();

        await searchInput.isDisplayed();
        await searchInput.setValue(ORDER_NUMBER);

        await searchButton.clickButton();

        await this.browser.waitUntil(async () => (await tableValueOrderNumber.link).isClickable(), {
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
        });
        await tableValueStatus.isDisplayed();

        const orderNumber = await (await tableValueOrderNumber.link).getText();

        expect(orderNumber).to.equal(ORDER_NUMBER, 'Заказ не найден');
        const orderStatus = await (await tableValueStatus.span).getText();

        expect(orderStatus).to.equal('отменен', 'Статус заказа не соответствует тест-кейсу');

        await attributesButton.isDisplayed();
        await attributesButton.clickButton();

        await tabBar.isDisplayed();
        await tabBar.clickTab('Настройки отображения');

        await coloredCircleWithTextPresentationStatusRadioButton.isDisplayed();
        await coloredCircleWithTextPresentationStatusRadioButton.click();

        await saveButton.isDisplayed();
        await saveButton.clickButton();

        const isSaveButtonInvisible = await saveButton.waitForInvisible();

        expect(isSaveButtonInvisible).to.equal(true, 'Модалка редактирования отображения атрибута не закрылась');

        await tableValueStatus.isDisplayed();
        const statusColor = await (await tableValueStatus.elem('a svg')).getAttribute('color');
        const statusText = await (await tableValueStatus.elem('a')).getText();

        expect([statusColor, statusText]).to.eql(
            [STATUS_COLOR, STATUS_TEXT],
            'Отображение атрибута типа CatalogItem не в нужном формате или цвете'
        );
    });
});
