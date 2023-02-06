import {order} from 'suites/trains';
import {assert} from 'chai';

import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.confirmation, () => {
    it('ЖД - Реквизиты компаний партнеров на странице подтверждения', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderConfirmationStepPage,
            orderPassengersStepPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();

        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext(TRAINS_SUCCESS_TEST_CONTEXT_PARAMS);

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();

        /**
         * Webdriver не понимает куда ему скролить, чтобы кликнуть на ссылку партнеры
         * поэтому проматываем в конец страницы
         */
        await orderConfirmationStepPage.layout.footer.scrollIntoView();

        assert.equal(
            await orderConfirmationStepPage.partnersRequisites.button.getText(),
            'партнёрами',
            'Должна присутствовать строка "партнёрами"',
        );

        await orderConfirmationStepPage.partnersRequisites.button.click();

        assert.isTrue(
            await orderConfirmationStepPage.partnersRequisites.modal.isVisible(),
            'Должно открыться модальное окно с реквизитами партнёров',
        );

        assert.equal(
            await orderConfirmationStepPage.partnersRequisites.partnersInfo.title.getText(),
            'Реквизиты',
            'Должен быть заголовок "Реквизиты" у модального окна',
        );

        const partners =
            orderConfirmationStepPage.partnersRequisites.partnersInfo.partners;

        assert.include(
            await (await partners.first()).getPartnerTitle(),
            'Перевозчик',
            'Должен присутствовать заголовок "Перевозчик"',
        );

        assert.isTrue(
            await (await partners.first()).checkAllFieldsFilled(),
            'Должны быть заполнены все поля для перевозчика',
        );

        assert.equal(
            await (await partners.at(1)).getPartnerTitle(),
            'Партнер ООО «Инновационная мобильность»',
            'Должно совпадать название перевозчика',
        );

        assert.isTrue(
            await (await partners.at(1)).checkAllFieldsFilled(),
            'Должны быть заполнены все поля для партнера',
        );

        assert.equal(
            await (await partners.at(2)).getPartnerTitle(),
            'Страховщик АО «Группа Ренессанс Страхование»',
            'Должно совпадать название перевозчика',
        );

        assert.isTrue(
            await (await partners.at(2)).checkAllFieldsFilled(),
            'Должны быть заполнены все поля для страховщика',
        );

        assert.equal(
            await (await partners.last()).getPartnerTitle(),
            'Сервис предоставляет ООО «Яндекс»',
            'Должно совпадать название перевозчика',
        );

        assert.isTrue(
            await (await partners.last()).checkAllFieldsFilled(),
            'Должны быть заполнены все поля для Яндекса',
        );

        await orderConfirmationStepPage.partnersRequisites.modal.closeButton.click();

        assert.isTrue(
            await orderConfirmationStepPage.partnersRequisites.button.isVisible(),
            'Должно закрыться модальное окно с информацией о партнёрах',
        );
    });
});
