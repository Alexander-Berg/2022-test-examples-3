import {order} from 'suites/trains';
import {assert} from 'chai';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import TestTrainItem from 'helpers/project/trains/components/TestOrderSummary/components/TestTrainItem/TestTrainItem';
import {
    PASSENGER,
    PASSENGER_BABY,
    PASSENGER_CHILD,
} from 'helpers/project/trains/data/passengers';

const BABY_PLACE_INDEX_IN_SUMMARY = 2;

async function checkTrainSummary({
    trainSummary,
    fromPointName,
    toPointName,
    isBeddingIncluded,
    isBeddingChecked,
    trainIndex,
}: {
    trainSummary: TestTrainItem;
    fromPointName: string;
    toPointName: string;
    isBeddingIncluded: boolean;
    isBeddingChecked: boolean;
    trainIndex: number;
}): Promise<void> {
    const humanTrainIndex = trainIndex === 0 ? 'первого' : 'второго';

    assert.equal(
        await trainSummary.title.getText(),
        `${fromPointName} — ${toPointName}`,
        `Должно отображаться верное направление ${humanTrainIndex} поезда в корзинке`,
    );

    assert.isTrue(
        await trainSummary.places.places.every(async (placeItem, index) => {
            return (
                (await placeItem.title.isDisplayed()) &&
                (index === BABY_PLACE_INDEX_IN_SUMMARY ||
                    (await placeItem.places.isDisplayed())) &&
                (await placeItem.price.isDisplayed())
            );
        }),
        `Должна отображаться информация (тариф, место, цена) о выбранных местах ${humanTrainIndex} поезда`,
    );

    const babyPlaceItem = await trainSummary.places.places.at(
        BABY_PLACE_INDEX_IN_SUMMARY,
    );

    assert.equal(
        await babyPlaceItem.price.getPriceValue(),
        0,
        `Стоимость для ребенка без места должна быть 0 руб. для ${humanTrainIndex} поезда`,
    );

    if (isBeddingIncluded) {
        assert.isTrue(
            await trainSummary.bedding.beddingIncluded.isDisplayed(),
            `В корзинке должна отображаться надпись, что белье включено в стоимость для ${humanTrainIndex} поезда`,
        );
    } else if (isBeddingChecked) {
        assert.isTrue(
            await trainSummary.bedding.isChecked(),
            `В корзинке должен отображаться и быть выбран чекбокс с выбранным постельным бельем для ${humanTrainIndex} поезда`,
        );
    }
}

describe(order.steps.confirmation, () => {
    it('ЖД+ЖД Корзинка в таче', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPlacesStepPage: {
                layout: {orderSummary: placesOrderSummary},
            },
            orderPassengersStepPage,
            orderConfirmationStepPage,
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
            await placesOrderSummary.title.getText(),
            'Стоимость билетов',
            'В корзинке должен отображаться верный заголовок',
        );

        assert.equal(
            await placesOrderSummary.orderButton.getText(),
            'Далее',
            'В корзинке должен отображаться верный текст у кнопки',
        );

        assert.equal(
            await (await placesOrderSummary.trains.at(0)).title.getText(),
            `${fromName} — ${transferName}`,
            'В корзинке должно отображаться верное направление для первого поезда',
        );

        assert.isFalse(
            await orderPlacesStepPage.orderSummaryCompact.isDisplayed(),
            'До выбора мест не должна отображаться корзинка под схемой',
        );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);

        assert.isTrue(
            await orderPlacesStepPage.orderSummaryCompact.isDisplayed(),
            'После выбора мест должна отобразиться корзинка под схемой',
        );

        assert.equal(
            await orderPlacesStepPage.orderSummaryCompact.places.getText(),
            '2 места',
            'Должно отображаться верное количество мест в компактной корзинке',
        );

        assert.isTrue(
            await orderPlacesStepPage.orderSummaryCompact.price.isDisplayed(),
            'Должна отображаться цена в компактной корзинке',
        );

        await orderPlacesStepPage.orderSummaryCompact.openOrderSummary();

        const detailedOrderSummary =
            orderPlacesStepPage.orderSummaryCompact.orderSummary;

        await checkTrainSummary({
            fromPointName: fromName,
            toPointName: transferName,
            trainSummary: await detailedOrderSummary.trains.at(0),
            isBeddingIncluded: false,
            isBeddingChecked: true,
            trainIndex: 0,
        });

        assert.approximately(
            await detailedOrderSummary.totalPrice.price.getPriceValue(),
            await detailedOrderSummary.getTotalPriceByTickets(),
            0.01,
            'Общая цена должна быть равна сумме цен за все билеты для первого поезда',
        );

        await detailedOrderSummary.orderButton.click();

        assert.equal(
            await (await placesOrderSummary.trains.at(1)).title.getText(),
            `${transferName} — ${toName}`,
            'В корзинке должно отображаться верное направление для второго поезда',
        );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);

        assert.equal(
            await orderPlacesStepPage.orderSummaryCompact.places.getText(),
            '2 места',
            'Должно отображаться верное количество мест в компактной корзинке',
        );

        assert.isTrue(
            await orderPlacesStepPage.orderSummaryCompact.price.isDisplayed(),
            'Должна отображаться цена в компактной корзинке',
        );

        await orderPlacesStepPage.orderSummaryCompact.openOrderSummary();

        const secondTrainSummary = await detailedOrderSummary.trains.at(1);

        await checkTrainSummary({
            fromPointName: transferName,
            toPointName: toName,
            trainSummary: secondTrainSummary,
            isBeddingIncluded: false,
            isBeddingChecked: true,
            trainIndex: 1,
        });

        const totalPriceOfBothTrains =
            await detailedOrderSummary.getTotalPriceByTickets();

        assert.approximately(
            await detailedOrderSummary.totalPrice.price.getPriceValue(),
            totalPriceOfBothTrains,
            0.01,
            'Общая цена должна быть равна сумме цен за все билеты для обоих поездов',
        );

        const secondTrainBeddingPrice =
            (await secondTrainSummary.bedding.getPrice()) ?? 0;

        await secondTrainSummary.bedding.option.checkbox.click();

        assert.approximately(
            await detailedOrderSummary.totalPrice.price.getPriceValue(),
            totalPriceOfBothTrains - secondTrainBeddingPrice,
            0.01,
            'Общая цена должна уменьшиться на стоимость белья для второго поезда',
        );

        await detailedOrderSummary.orderButton.click();

        await orderPassengersStepPage.fillPassengers([
            PASSENGER,
            PASSENGER_CHILD,
            PASSENGER_BABY,
        ]);

        await orderPassengersStepPage.fillContacts();

        await app.setTestContext();

        await orderPassengersStepPage.layout.goNextStep();
        await orderConfirmationStepPage.waitOrderLoaded();

        await orderConfirmationStepPage.layout.orderSummary.trains.every(
            async (train, index) => {
                await checkTrainSummary({
                    fromPointName: index === 0 ? fromName : transferName,
                    toPointName: index === 0 ? transferName : toName,
                    trainSummary: train,
                    isBeddingIncluded: index === 0,
                    isBeddingChecked: false,
                    trainIndex: index,
                });

                return true;
            },
        );

        await orderConfirmationStepPage.layout.orderSummary.goToConfirmStep?.clickButtonAndAwaitAnimation();

        const confirmationOrderPrice =
            await orderConfirmationStepPage.layout.orderSummary.getTotalPriceByTickets();

        assert.approximately(
            await orderConfirmationStepPage.layout.orderSummary.totalPrice.price.getPriceValue(),
            confirmationOrderPrice,
            0.01,
            'Общая цена должна быть равна сумме цен за все билеты для обоих поездов',
        );

        const insurancePrice =
            await orderConfirmationStepPage.layout.orderSummary.insurance.price.getPriceValue();

        await orderConfirmationStepPage.layout.orderSummary.insurance.checkbox.click();

        assert.approximately(
            await orderConfirmationStepPage.layout.orderSummary.totalPrice.price.getPriceValue(),
            confirmationOrderPrice + insurancePrice,
            0.01,
            'Общая цена должна увеличиться на стоимость страховки',
        );
    });
});
