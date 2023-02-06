import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import Button from '../../page-objects/button';
import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import ContentWithLabel from '../../page-objects/contentWithLabel';

const PAGE_URL = '/entity/root@1';

/**
 * Проверяем, что:
 * После выбора буквенного формата для атрибута "Ожидает назначения на оператора" типа boolean
 * в настройках отображения письменной коммуникации,
 * атрибут отображается в формате "да" / "нет"
 */
describe(`ocrm-1547: Отображение атрибута типа boolean текстом`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`После настройки отображения атрибут "Ожидает назначения на оператора" должен быть в буквенном представлении`, async function() {
        const attributesButton = new Button(this.browser, 'body', '[data-ow-test-table-toolbar="table-attributes"]');

        const waitDistributionListItemCheckbox = new ContentWithLabel(
            this.browser,
            '[data-ow-test-inactive-list-item="waitDistribution"]',
            '[data-ow-test-checkbox]'
        );

        const tabBar = new TabsWrapper(
            this.browser,
            '[data-ow-test-settings-modal="tab-bar-content"]',
            '[data-ow-test-attribute-container="tabsWrapper"]'
        );

        const literalPresentationWaitDistributionRadioButton = new ContentWithLabel(
            this.browser,
            '[data-ow-test-radio-group="presentationOf_waitDistribution"]',
            '[data-ow-test-radio="literal"]'
        );

        const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');

        const tableValueWaitDistribution = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-table-row-0="waitDistribution"]'
        );

        await attributesButton.isDisplayed();
        await attributesButton.clickButton();

        await waitDistributionListItemCheckbox.isExisting();
        await waitDistributionListItemCheckbox.click();

        await tabBar.isDisplayed();
        await tabBar.clickTab('Настройки отображения');

        await literalPresentationWaitDistributionRadioButton.isDisplayed();
        await literalPresentationWaitDistributionRadioButton.click();

        await saveButton.isDisplayed();
        await saveButton.clickButton();

        const isSaveButtonInvisible = await saveButton.waitForInvisible();

        expect(isSaveButtonInvisible).to.equal(true, 'Модалка редактирования отображения атрибута не закрылась');

        await tableValueWaitDistribution.isDisplayed();
        const waitDistributionSpanText = await (await tableValueWaitDistribution.span).getText();

        expect(waitDistributionSpanText).to.be.oneOf(
            ['да', 'нет'],
            'Отображение атрибута типа boolean не в нужном формате'
        );
    });
});
