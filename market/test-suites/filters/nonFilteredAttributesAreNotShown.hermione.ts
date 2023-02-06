import {expect} from 'chai';

import {AttributeFqnSelectPageObject} from '../../../src/components/jmf/AttributeFqnSelect/__pageObject__';
import {AddFilterButtonPageObject} from '../../../src/components/Filters/AddFilterButton/__pageObject__';
import Button from '../../page-objects/button';
import {login, PageObject} from '../../helpers';

const ROOT_PAGE_URL = '/entity/root@1';

const FILTERABLE_ATTRIBUTES: string[] = ['gid', 'archived', 'brand'];

const NON_FILTERABLE_ATTRIBUTES: string[] = ['activeStatuses', 'processing', 'ticketNumber'];

/** Функция открытия модального окна и создание фильтра "Проверка атрибута" */
const openFilterModalAndCreateAttributeFilter = async (context: WebdriverIO.Browser): Promise<void> => {
    const openEditFiltersModalButton = new Button(context, 'body', '[data-ow-test-table-toolbar="table-filter"]');
    const editFiltersModalContent = new PageObject(context, 'body', '[data-ow-test-table-filters="modal-content"]');
    const addFilterButton = new AddFilterButtonPageObject(
        context,
        'body',
        '[data-ow-test-filters="add-filter-button"]'
    );

    await openEditFiltersModalButton.waitForEnable(
        'Не дождались появления кнопки открытия модального окна редактирования фильтров'
    );
    await openEditFiltersModalButton.clickButton();

    await editFiltersModalContent.isDisplayed('Не дождались появления модального окна редактирования фильтров');
    await addFilterButton.isDisplayed('Не дождались появления кнопки добавления фильтра');

    await addFilterButton.createFilter('Проверка атрибута');
};

describe('ocrm-1652: Не фильтруемые атрибуты скрыты в окне настройки фильтров таблиц', () => {
    beforeEach(function() {
        return login(ROOT_PAGE_URL, this);
    });

    FILTERABLE_ATTRIBUTES.forEach(attribute => {
        it(`Фильтруемый атрибут "${attribute}" отображается в списке.`, async function() {
            await openFilterModalAndCreateAttributeFilter(this.browser);

            const addNewAttributePathItem = new AttributeFqnSelectPageObject(
                this.browser,
                'body',
                '[data-ow-test-attribute-path-selector="new-path-item"]'
            );

            const hasAttributeForSelect = await addNewAttributePathItem.hasAttributeForSelect(attribute, 20000);

            expect(hasAttributeForSelect).to.equal(true, 'Нельзя выбрать фильтруемый атрибут');
        });
    });

    NON_FILTERABLE_ATTRIBUTES.forEach(attribute => {
        it(`Не фильтруемый атрибут "${attribute}" не отображается в списке.`, async function() {
            await openFilterModalAndCreateAttributeFilter(this.browser);

            const addNewAttributePathItem = new AttributeFqnSelectPageObject(
                this.browser,
                'body',
                '[data-ow-test-attribute-path-selector="new-path-item"]'
            );

            const hasAttributeForSelect = await addNewAttributePathItem.hasAttributeForSelect(attribute, 20000);

            expect(hasAttributeForSelect).to.equal(false, 'Можно выбрать не фильтруемый атрибут');
        });
    });
});
