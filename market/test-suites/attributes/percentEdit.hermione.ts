import 'hermione';
import {expect} from 'chai';

import {execScript, login} from '../../helpers';
import Button from '../../page-objects/button';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {CLEAR_ALL_SEQUENCE, CLEAR_SEQUENCE, VERY_LONG_TIMEOUT_MS} from '../../constants';
import {PROPERTIES_LIST_ACTION_WRAPPER_LABEL} from '../../../src/components/jmf/PropertiesList/constants';

const TICKET_GID = 'ticket@204034769';

const createCashCompensationScript = (ticketGid: string, cashCompensation: number): string =>
    `api.bcp.edit('${ticketGid}', ['cashCompensation': ${cashCompensation}])`;

/**
 * Проверяем, что:
 * На вкладке "Информация о ПВЗ" при редактировании в "Тарифах"
 * атрибута "Компенсации за инкассацию" типа Percent можно сохранить пустое значение.
 */
describe(`ocrm-1540: Очистка значений полей в блоке "Тарифы"`, () => {
    beforeEach(function() {
        const url = `entity/${TICKET_GID}`;

        return login(url, this);
    });

    it(`При редактировании "Компенсации за инкассацию" в "Тарифах" должно сохраняться пустое значение`, async function() {
        await execScript(this.browser, createCashCompensationScript(TICKET_GID, 0.2));

        await this.browser.refresh();

        const tabBar = new TabsWrapper(
            this.browser,
            '[data-ow-test-content="tabBar"]',
            '[data-ow-test-attribute-container="tabsWrapper"]'
        );

        const editButton = new Button(
            this.browser,
            '[data-ow-test-content="properties-tariffs"]',
            '[data-ow-test-properties-list-toolbar="edit"]'
        );

        const cashCompensationEdit = new ContentWithLabel(
            this.browser,
            '[data-ow-test-content="editProperties-tariffs"]',
            '[data-ow-test-attribute-container="cashCompensation"]'
        );

        const saveButton = new Button(
            this.browser,
            'body',
            `[data-ow-test-${PROPERTIES_LIST_ACTION_WRAPPER_LABEL}="save"]`
        );

        await tabBar.isDisplayed(undefined, VERY_LONG_TIMEOUT_MS);
        await tabBar.clickTab('Информация о ПВЗ');
        await editButton.clickButton();
        await saveButton.isExisting();
        const input = await cashCompensationEdit.input;

        await input.isDisplayed();
        await input.doubleClick();
        await cashCompensationEdit.setValue(Array(10).fill(CLEAR_SEQUENCE));
        await cashCompensationEdit.setValue(CLEAR_ALL_SEQUENCE);

        await saveButton.isDisplayed();
        await saveButton.clickButton();

        const isCashCompensationEditInvisible = await cashCompensationEdit.waitForInvisible();

        expect(isCashCompensationEditInvisible).to.equal(true, 'Модалка редактирования тарифов не закрылась');
    });
});
