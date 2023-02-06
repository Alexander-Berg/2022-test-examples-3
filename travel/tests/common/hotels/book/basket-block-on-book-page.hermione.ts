import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';
import _sum from 'lodash/sum';

import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsBookPage} from 'helpers/project/hotels/pages/TestHotelsBookPage/TestHotelsBookPage';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    it('Корзина заказа на странице бронирования', async function () {
        const testOfferParams = getTestOfferParams({
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(3, 'days').format(dateFormats.ROBOT),
            priceAmount: 1000,
        });

        const hotelsBookPage = new TestHotelsBookPage(this.browser);

        await hotelsBookPage.goToPage(testOfferParams);

        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        assert.isTrue(
            await hotelsBookPage.priceInfo.isVisible(),
            'Не отобразилась корзинка стоимости',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.title.isVisible(),
            'Не отобразился заголовок блока',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.nightCountButton.isVisible(),
            'Не отобразилось общее кол-во ночей',
        );

        const nigthsByParams = '2 ночи';
        const nigthsByPage =
            await hotelsBookPage.priceInfo.nightCountButton.getText();

        assert.equal(
            nigthsByParams,
            nigthsByPage,
            `Не совпадает кол-во ночей в запросе и на странице.
                nigthsByParams: ${nigthsByParams}.
                nigthsByPage: ${nigthsByPage}.
            `,
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.nightsTotal.price.isVisible(),
            'Не отобразилась сумма за все ночи',
        );

        await hotelsBookPage.priceInfo.nightCountButton.scrollIntoView();

        await hotelsBookPage.priceInfo.nightCountButton.click();

        assert.isAbove(
            await hotelsBookPage.priceInfo.nightPrices.count(),
            0,
            'Нет блока детализации ночей',
        );

        await hotelsBookPage.priceInfo.nightPrices.forEach(async nightPrice => {
            assert.isTrue(
                await nightPrice.isVisible(),
                'Не отобразилась информация за одну ночь',
            );
            assert.isTrue(
                await nightPrice.price.isVisible(),
                'Не отобразилась цена одной ночи',
            );
        });

        const prices = await hotelsBookPage.priceInfo.nightPrices.map(
            async nightPrice => {
                return nightPrice.price.getPriceValue();
            },
        );

        assert.equal(
            _sum(prices),
            testOfferParams.priceAmount,
            `Не совпадает итоговая цена оффера с ценой в запросе.
                sumAllNights: ${_sum(prices)}.
                totalPriceByParams: ${testOfferParams.priceAmount}.
            `,
        );

        await hotelsBookPage.priceInfo.promoCodes.checkBox.scrollIntoView();

        assert.isTrue(
            await hotelsBookPage.priceInfo.isVisible(),
            'Не отобразилась подпись к чекбоксу промокода',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.totalPriceLabel.isVisible(),
            'Не отобразился лэйбл итоговой суммы',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.totalPrice.isVisible(),
            'Не отобразилась итоговая сумма',
        );

        const totalPriceByPage =
            await hotelsBookPage.priceInfo.totalPrice.getPriceValue();
        const totalPriceByPageAllNights =
            await hotelsBookPage.priceInfo.nightsTotal.price.getPriceValue();

        assert.equal(
            totalPriceByPage,
            testOfferParams.priceAmount,
            `Не совпадает итоговая цена оффера с ценой в запросе.
                totalPriceByPage: ${totalPriceByPage}.
                totalPriceByParams: ${testOfferParams.priceAmount}.
            `,
        );

        assert.equal(
            totalPriceByPage,
            totalPriceByPageAllNights,
            `Не совпадает итоговая цена оффера с ценой за все ночи.
                totalPriceByPage: ${totalPriceByPage}.
                totalPriceByPageAllNights: ${totalPriceByPageAllNights}.
            `,
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.plusInfo.label.isVisible(),
            'Не отобразился лэйбл плюса',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.plusInfo.triggerDetailsButton.isVisible(),
            'Не отобразилась иконка у лэйбла плюса',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.plusInfo.plusPoints.isVisible(),
            'Не отобразились баллы плюса',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.submitButton.isVisible(),
            'Не отобразилась кнопка оплаты',
        );

        assert.isTrue(
            await hotelsBookPage.priceInfo.cancellationInfo.trigger.isVisible(),
            'Не отобразилась инфа о бесплатной отмене',
        );

        const cancellationTriggerDateAndDescriptionByPage =
            await hotelsBookPage.priceInfo.cancellationInfo.getTriggerText();

        const cancellationTriggerByParams = `Бесплатная отмена до ${moment(
            testOfferParams.checkinDate,
        ).format(dateFormats.HUMAN)}`;

        assert.equal(
            cancellationTriggerDateAndDescriptionByPage,
            cancellationTriggerByParams,
            `Не совпадает параметр cancellationTrigger.
                cancellationTriggerByPage: ${cancellationTriggerDateAndDescriptionByPage}.
                cancellationTriggerByParams: ${cancellationTriggerByParams}.
            `,
        );
    });
});
