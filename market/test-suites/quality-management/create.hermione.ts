import 'hermione';
import chai, {expect} from 'chai';
import chaiUrl from 'chai-url';

import CardHeader from '../../../src/modules/jmfEntity/components/CardHeader/__pageObject__';
import CreateButton from '../../page-objects/createButton';
import CustomSelectionHead from '../../../src/components/jmf/CustomSelectionHead/__pageObject__';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import AttributePopup from '../../page-objects/attributePopup';
import SaveButton from '../../page-objects/saveButton';
import {login} from '../../helpers';
import {MAIN_PAGE_PATH, CREATION_PAGE_PATH} from './config';

chai.use(chaiUrl);

// id: 'ocrm-889'
// issue: 'OCRM-6051'
describe('ocrm-889: Статусы выборок', () => {
    describe('Страница "Создание выборки"', () => {
        beforeEach(function() {
            return login(MAIN_PAGE_PATH, this);
        });

        it('должна содержать корректный заголовок', async function() {
            const cardHeader = new CardHeader(this.browser, 'body', '[data-ow-test-card-header="default"]');
            const expectedText = 'Контроль качества';

            await cardHeader.isDisplayed();
            const titleText = await cardHeader.getTitleText();

            expect(titleText, 'Заголовок неверный').to.equal(expectedText);
        });
    });

    describe('Кнопка создания выборки', () => {
        beforeEach(function() {
            return login(MAIN_PAGE_PATH, this);
        });

        it('должна открыть страницу создания при нажатии', async function() {
            const createButton = new CreateButton(this.browser);

            await createButton.isDisplayed();
            await createButton.clickButton();

            const currentUrl = await this.browser.getUrl();

            expect(currentUrl, 'Ссылка неверная').to.contain.path(CREATION_PAGE_PATH);
        });
    });

    describe('Создание выборки', () => {
        beforeEach(function() {
            return login(CREATION_PAGE_PATH, this);
        });

        it('должно завершаться страницей выборки со статусом "новый"', async function() {
            const cardCustomHeader = new CustomSelectionHead(this.browser);
            const cardHeader = new CardHeader(this.browser, 'body', '[data-ow-test-card-header="default"]');
            const saveButton = new SaveButton(
                this.browser,
                'body',
                '[data-ow-test-save-button="qualityManagementSelection"]'
            );
            const contentWithLabelTitle = new ContentWithLabel(
                this.browser,
                'body',
                '[data-ow-test-attribute-container="title"]'
            );
            const contentWithLabelTicketsMaxNumber = new ContentWithLabel(
                this.browser,
                'body',
                '[data-ow-test-attribute-container="ticketsMaxNumber"]'
            );
            const contentWithLabelChannels = new ContentWithLabel(
                this.browser,
                'body',
                '[data-ow-test-attribute-container="channels"]'
            );
            const contentWithLabelResponsibles = new ContentWithLabel(
                this.browser,
                'body',
                '[data-ow-test-attribute-container="responsibles"]'
            );
            const attributePopupChannels = new AttributePopup(
                this.browser,
                'body',
                '[data-ow-test-attribute-popup="channels"]'
            );
            const attributePopupResponsibles = new AttributePopup(
                this.browser,
                'body',
                '[data-ow-test-attribute-popup="responsibles"]'
            );

            const expectedText = 'Выборка контроля качества';

            await cardHeader.isDisplayed();
            const titleText = await cardHeader.getTitleText();

            await expect(titleText).to.be.equal(expectedText, 'Заголовок страницы правильный');

            await contentWithLabelTitle.setValue('Тест названия');

            await contentWithLabelTicketsMaxNumber.setValue('100');

            (await contentWithLabelChannels.input).click();

            await attributePopupChannels.clickFirstElement();

            (await contentWithLabelResponsibles.input).click();

            await attributePopupResponsibles.clickFirstElement();

            await saveButton.clickButton();

            await cardCustomHeader.isDisplayed();

            const expectedStatusText = 'Новая';
            const statusMarkerText = await cardCustomHeader.getStatusMarkerText();

            return expect(statusMarkerText).to.be.equal(
                expectedStatusText,
                'Текст маркера созданной выборки корректный'
            );
        });
    });
});
