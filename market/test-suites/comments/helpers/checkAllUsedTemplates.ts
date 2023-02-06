import 'hermione';
import {expect} from 'chai';

import {login} from '../../../helpers';
import Button from '../../../page-objects/button';
import SelectValue from '../../../page-objects/selectValue';
import AttributeValue from '../../../page-objects/attributeValue';
import ContentWithLabel from '../../../page-objects/contentWithLabel';

const PAGE_URL = '/entity/service@30013907?tabBar=1';

export const checkAllUsedTemplates = async (ctx, ticketTitle: string, templateTitle: string) => {
    await login(PAGE_URL, ctx);

    const tableViewSelector = new Button(ctx.browser, 'body', '[data-ow-test-table-toolbar="table-view-select"]');
    const selectValue = new SelectValue(
        ctx.browser,
        'body',
        '[data-ow-test-select-option="[Автотест] Информация о примененных шаблонах"]'
    );

    const searchInput = new ContentWithLabel(ctx.browser, 'body', '[data-ow-test-table-toolbar="search-input"]');

    const usedTemplates = new AttributeValue(ctx.browser, 'body', '[data-ow-test-table-row-0="allUsedTemplates"]');

    await tableViewSelector.clickButton();
    await selectValue.clickSelectValue();

    await searchInput.setValue(ticketTitle);

    const template = await usedTemplates.getText();

    expect(template).to.equal(templateTitle);
};
