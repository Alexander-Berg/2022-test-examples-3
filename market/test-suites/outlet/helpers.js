import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import outlet1 from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/outlet1';
import alcohol from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/alcohol';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';
import {buildFormattedPrice} from '@self/root/src/entities/price';
import {buildDeliveryPriceConditionText} from '@self/root/src/utils/delivery';

export const nonBrandedOutletMock = alcohol;
export const brandedOutletMock = outlet1;
export const OUTLET_ID = nonBrandedOutletMock.id;

export function openOutletPage() {
    return this.browser.yaOpenPage(PAGE_IDS_COMMON.OUTLET_PAGE, {
        outletId: OUTLET_ID,
    });
}

export function checkActionBlockVisibility() {
    return this.outletActions.waitForVisible()
        .should.eventually.be.equal(
            true,
            'Блок с кнопками действий должен быть виден'
        );
}

export function checkAddToFavoritesButtonText(buttonText) {
    return this.primaryButton.getButtonText()
        .should.eventually.be.equal(
            buttonText,
            `Текст кнопки добавления в избранное должен быть "${buttonText}"`
        );
}

export function getDeliveryConditionsCorrectText() {
    const deliveryRates = deliveryConditionMock.base.deliveryRates;

    const separator = '\n';

    const deliveryRatesTexts = deliveryRates.map(({minOrderPrice, maxOrderPrice, price}) => {
        const conditionText = buildDeliveryPriceConditionText({
            orderPrice: {
                from: minOrderPrice,
                to: maxOrderPrice,
            },
        });

        const priceText = price.value === '0' ? 'бесплатно' : `${buildFormattedPrice(price)}`;

        return `${conditionText}${separator}${priceText}`;
    });

    /**
     * Приходится заменять неразрывные пробелы обычными,
     * так как selenium отдаёт вместо неразрывных пробелов обычные
     */
    return deliveryRatesTexts.join(separator).replace(/\u00a0/g, ' ');
}
