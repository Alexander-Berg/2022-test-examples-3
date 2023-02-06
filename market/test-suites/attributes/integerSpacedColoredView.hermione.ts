import 'hermione';
import {expect} from 'chai';

import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {login} from '../../helpers';
import Button from '../../page-objects/button';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {CHECK_INTERVAL, TIMEOUT_MS} from '../../constants';
import {Locale} from '../../../src/constants/locale';

const PAGE_URL = '/entity/root@1?tabBar=8';
const NEGATIVE_SUPPLIER_ID = '-11111111';
const SUPPLIER_ID_COLOR = 'rgba(241,54,48,1)';

const selectOnlySuppliersAndChoosePresentation = async (browser, presentation): Promise<void> => {
    const attributesButton = new Button(browser, 'body', '[data-ow-test-table-toolbar="table-attributes"]');
    const tabBar = new TabsWrapper(
        browser,
        '[data-ow-test-settings-modal="tab-bar-content"]',
        '[data-ow-test-attribute-container="tabsWrapper"]'
    );
    const tableMetaclassSelector = await browser.$('[data-ow-test-table-metaclass-selector] div');
    const selectSupplierOptionButton = await browser.$('[data-ow-test-select-option="Синий магазин"] span');

    const supplierIdListItemCheckbox = new ContentWithLabel(
        browser,
        '[data-ow-test-inactive-list-item="supplierId"]',
        '[data-ow-test-checkbox]'
    );
    const presentationRadioButton = new ContentWithLabel(
        browser,
        '[data-ow-test-radio-group="presentationOf_supplierId"]',
        `[data-ow-test-radio="${presentation}"]`
    );
    const saveButton = new Button(browser, 'body', '[data-ow-test-modal-controls="save"]');

    await attributesButton.isDisplayed();
    await attributesButton.clickButton();

    await tabBar.isDisplayed();
    await tabBar.clickTab('Настройки');
    await tableMetaclassSelector.isDisplayed();
    await tableMetaclassSelector.click();
    await selectSupplierOptionButton.isDisplayed();
    await selectSupplierOptionButton.click();

    await tabBar.clickTab('Атрибуты');
    await supplierIdListItemCheckbox.isExisting();
    await supplierIdListItemCheckbox.click();
    await tabBar.clickTab('Настройки отображения');

    await presentationRadioButton.isDisplayed();
    await presentationRadioButton.click();

    await saveButton.isDisplayed();
    await saveButton.clickButton();
    const isSaveButtonInvisible = await saveButton.waitForInvisible();

    expect(isSaveButtonInvisible).to.equal(true, 'Модалка редактирования отображения атрибута не закрылась');
};

/**
 * Проверяем, что:
 * После выбора spaced формата для атрибута "Supplier ID" типа integer
 * в настройках отображения таблицы b2b партнеров
 * между разрядами в числах будет стоять пробел.
 */
describe(`ocrm-1589: Представление для атрибутов типа integer - spaced`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`После настройки отображения атрибут "Supplier ID" должен быть в представлении spaced`, async function() {
        const tableValueSupplierId = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-table-row-0="supplierId"]'
        );

        await selectOnlySuppliersAndChoosePresentation(this.browser, 'spaced');

        await tableValueSupplierId.isDisplayed();
        const supplierIdSpanText = await (await tableValueSupplierId.span).getText();
        const supplierId = supplierIdSpanText.replace(/\s/g, '');
        const supplierIdWithSpaces = String(new Intl.NumberFormat(Locale.RU).format(Number(supplierId))).replace(
            /[\s,]/g,
            ' '
        );

        expect(supplierIdSpanText).to.equal(
            supplierIdWithSpaces,
            'Отображение атрибута типа integer не в нужном формате'
        );
    });
});

/**
 * Проверяем, что:
 * После выбора colored формата для атрибута "Supplier ID" типа integer
 * в настройках отображения таблицы b2b партнеров
 * отрицательное значение будет выделено красным цветом.
 */
describe(`ocrm-1590: Представление для атрибутов типа integer - colored`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`После настройки отображения атрибут "Supplier ID" должен быть в представлении colored`, async function() {
        const searchInput = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-toolbar="search-input"]');
        const searchButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="search-button"]');
        const tableValueSupplierId = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-table-row-0="supplierId"]'
        );
        const tableValueTitle = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-row-0="title"]');

        await selectOnlySuppliersAndChoosePresentation(this.browser, 'colored');

        await searchInput.isDisplayed();
        await searchInput.setValue(NEGATIVE_SUPPLIER_ID);
        await searchButton.clickButton();
        await this.browser.waitUntil(async () => (await tableValueTitle.link).isClickable(), {
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
        });
        await tableValueSupplierId.isDisplayed();
        const color = await (await tableValueSupplierId.span).getCSSProperty('color');
        const supplierIdSpanText = await (await tableValueSupplierId.span).getText();
        const supplierId = supplierIdSpanText.replace(/ /g, '');

        expect(supplierId).to.equal(NEGATIVE_SUPPLIER_ID, 'SupplierID не соответствует тест-кейсу');

        expect(String(color.value)).to.equal(
            SUPPLIER_ID_COLOR,
            'Отображение атрибута типа integer не в нужном формате'
        );
    });
});

/**
 * Проверяем, что:
 * После выбора spacedColored формата для атрибута "Supplier ID" типа integer
 * в настройках отображения таблицы b2b партнеров
 * между разрядами в числах будет стоять пробел, отрицательное значение будет выделено красным цветом.
 */
describe(`ocrm-1591: Представление для атрибутов типа integer - spacedColored`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`После настройки отображения атрибут "Supplier ID" должен быть в представлении spacedColored`, async function() {
        const searchInput = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-toolbar="search-input"]');
        const searchButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="search-button"]');

        const tableValueSupplierId = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-table-row-0="supplierId"]'
        );
        const tableValueTitle = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-row-0="title"]');

        await selectOnlySuppliersAndChoosePresentation(this.browser, 'spacedColored');

        await searchInput.isDisplayed();
        await searchInput.setValue(NEGATIVE_SUPPLIER_ID);
        await searchButton.clickButton();
        await this.browser.waitUntil(async () => (await tableValueTitle.link).isClickable(), {
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
        });
        await tableValueSupplierId.isDisplayed();
        const color = await (await tableValueSupplierId.span).getCSSProperty('color');
        const supplierIdSpanText = await (await tableValueSupplierId.span).getText();
        const supplierId = supplierIdSpanText.replace(/ /g, '');
        const supplierIdWithSpaces = String(new Intl.NumberFormat(Locale.RU).format(Number(supplierId))).replace(
            /[\s,]/g,
            ' '
        );

        expect(supplierId).to.equal(NEGATIVE_SUPPLIER_ID, 'SupplierID не соответствует тест-кейсу');

        expect([String(color.value), supplierIdSpanText]).to.eql(
            [SUPPLIER_ID_COLOR, supplierIdWithSpaces],
            'Отображение атрибута типа integer не в нужном формате'
        );
    });
});
