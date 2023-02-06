import {AddFilterButtonPageObject} from '../../../../src/components/Filters/AddFilterButton/__pageObject__';
import ContentWithLabel from '../../../page-objects/contentWithLabel';
import Autocomplete from '../../../page-objects/autocomplete';

const TITLE = 'test';

export const fillRequiredData = async ctx => {
    const title = new ContentWithLabel(ctx.browser, 'body', '[data-ow-test-properties-list-attribute="title"]');

    const objectsMetaclass = new Autocomplete(
        ctx.browser,
        'body',
        '[data-ow-test-properties-list-attribute="objectsMetaclass"]'
    );

    const addFilterButton = new AddFilterButtonPageObject(
        ctx.browser,
        'body',
        '[data-ow-test-filters="add-filter-button"]'
    );

    await title.isDisplayed();
    await title.setValue(TITLE);

    await objectsMetaclass.isDisplayed();
    await objectsMetaclass.selectSingleItem('Правило автоматизации с запуском по расписанию');

    await addFilterButton.isDisplayed();
    await addFilterButton.createFilter('Проверка атрибута');
};
