// eslint-disable-next-line max-len
import CashbackPercentTextPO from '@self/root/src/components/CashbackPercentText/__pageObject';
import {EditableCardContent} from '@self/root/src/components/EditableCard/__pageObject/testamentPageObject';

import {delay} from '@self/root/src/spec/unit/utils/delay';
import {initContext} from '../helpers.js';

const widgetPath = '@self/root/src/widgets/content/checkout/common/EditPaymentOption';

export const checkPromoPaymentSystemIndicator = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    isDisplayed
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    // Ожидаем инициализацию компонента и загрузки карт
    await delay(1000);

    const editableCardPO = container.querySelector(EditableCardContent.root);
    editableCardPO.click();

    // Ожидаем открытие модала
    await delay(1000);

    const informer = container.querySelector(CashbackPercentTextPO.root);

    if (isDisplayed) {
        expect(informer).not.toBeNull();
        expect(informer.textContent).toEqual(' 10%');
    } else {
        expect(informer).toBeNull();
    }
};
