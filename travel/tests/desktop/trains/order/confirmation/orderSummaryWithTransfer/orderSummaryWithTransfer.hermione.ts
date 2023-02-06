import {order} from 'suites/trains';
import {assert} from 'chai';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {
    PASSENGER,
    PASSENGER_BABY,
    PASSENGER_CHILD,
} from 'helpers/project/trains/data/passengers';

describe(order.steps.confirmation, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('ЖД+ЖД Корзинка в десктопе', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPlacesStepPage: {
                layout: {orderSummary},
            },
            orderPassengersStepPage,
            orderConfirmationStepPage,
            orderConfirmationStepPage: {
                layout: {orderSummary: confirmationOrderSummary},
            },
        } = app;

        const {fromName, toName, transferName} =
            await orderPlacesStepPage.browseToPageWithTransfer();

        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({
            adults: 1,
            children: 1,
            babies: 1,
        });

        assert.equal(
            await orderSummary.title.getText(),
            'Стоимость билетов',
            'В корзинке должен отображаться верный заголовок',
        );

        assert.equal(
            await orderSummary.orderButton.getText(),
            'Далее',
            'В корзинке должен отображаться верный текст у кнопки',
        );

        const firstTrainItem = await orderSummary.trains.at(0);

        assert.equal(
            await firstTrainItem.title.getText(),
            `${fromName} — ${transferName}`,
            'В корзинке должно отображаться верное направление для первого поезда',
        );

        /* Требования к местам пока на холде */

        // await orderPlacesStepPage.selectPlatzkartePlacesWithRequirements();

        // assert.equal(
        //     await firstTrainItem.placesPlaceholder.getText(),
        //     'Будет известна на шаге подтверждения данных.',
        //     'После выбора требований к местам, в корзинке должна отображаться приписка, что цена будет известна только на странице подтверждения',
        // );

        // assert.isTrue(
        //     await firstTrainItem.bedding.isChecked(),
        //     'После выбора требований к местам, в корзинке появился выставленные чекбокс с бельем',
        // );
        //
        // assert.isFalse(
        //     await orderSummary.totalPrice.isDisplayed(),
        //     'При выборе требований к местам, не должна отображаться общая сумма до страницы подтверждения',
        // );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);

        await orderPlacesStepPage.goNextStep();

        const secondTrainItem = await orderSummary.trains.at(1);

        assert.equal(
            await secondTrainItem.title.getText(),
            `${transferName} — ${toName}`,
            'В корзинке должно отображаться верное направление для второго поезда',
        );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);

        assert.isTrue(
            await secondTrainItem.places.places.every(async placeItem => {
                return (
                    (await placeItem.title.isDisplayed()) &&
                    (await placeItem.places.isDisplayed()) &&
                    (await placeItem.price.isDisplayed())
                );
            }),
            'Должна отображаться информация (тариф, место, цена) о выбранных местах',
        );

        const babyPlaceItem = await secondTrainItem.places.places.at(2);

        assert.equal(
            await babyPlaceItem.price.getPriceValue(),
            0,
            'Стоимость для ребенка без места должна быть 0 руб.',
        );

        assert.isTrue(
            await secondTrainItem.bedding.isChecked(),
            'После выбора мест во втором поезде, в корзинке появился выставленные чекбокс с бельем для второго поезда',
        );

        // assert.isFalse(
        //     await orderSummary.totalPrice.isDisplayed(),
        //     'При выборе требований к местам, не должна отображаться общая сумма до страницы подтверждения',
        // );

        await secondTrainItem.bedding.option.checkbox.click();

        assert.isFalse(
            await secondTrainItem.bedding.isChecked(),
            'Чекбокс постельного белья должен быть отжат после нажатия по нему',
        );

        await orderPlacesStepPage.goNextStep();

        await orderPassengersStepPage.fillPassengers([
            PASSENGER,
            PASSENGER_CHILD,
            PASSENGER_BABY,
        ]);

        await orderPassengersStepPage.fillContacts();
        await app.setTestContext();
        await orderPassengersStepPage.layout.goNextStep();
        await orderConfirmationStepPage.waitOrderLoaded();

        assert.equal(
            await orderConfirmationStepPage.layout.orderSummary.trains.count(),
            2,
            'На странице подтверждения должна быть информация о двух поездах',
        );

        const [firstConfirmationTrainItem, secondConfirmationTrainItem] =
            await confirmationOrderSummary.trains.items;

        assert.equal(
            await firstConfirmationTrainItem.title.getText(),
            `${fromName} — ${transferName}`,
            'В корзинке должно отображаться верное направление для первого поезда на странице направления',
        );

        assert.equal(
            await secondConfirmationTrainItem.title.getText(),
            `${transferName} — ${toName}`,
            'В корзинке должно отображаться верное направление для второго поезда на странице направления',
        );

        assert.isTrue(
            await firstConfirmationTrainItem.bedding.beddingIncluded.isDisplayed(),
            'В корзинке для первого поезда должна быть информация, что белье включено',
        );

        assert.isFalse(
            await secondConfirmationTrainItem.bedding.beddingIncluded.isDisplayed(),
            'В корзинке для второго поезда не должно быть информации, что белье включено',
        );

        const totalPrice =
            await confirmationOrderSummary.getTotalPriceByTickets();

        assert.approximately(
            await confirmationOrderSummary.totalPrice.price.getPriceValue(),
            totalPrice,
            0.01,
            'Итоговая цена должна совпадать с суммой цен на все билеты',
        );

        const insurancePrice =
            await confirmationOrderSummary.insurance.price.getPriceValue();

        await confirmationOrderSummary.insurance.checkbox.click();

        assert.approximately(
            await confirmationOrderSummary.totalPrice.price.getPriceValue(),
            totalPrice + insurancePrice,
            0.01,
            'При добавлении страховки, итоговая цена должна увеличиться на сумму страховки',
        );
    });
});
