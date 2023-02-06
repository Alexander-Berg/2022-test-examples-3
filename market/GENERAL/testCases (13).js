import OrderTotal, {
    CashbackEmitTotal as CashbackEmitTotalPO,
    CashbackSpendTotal as CashbackSpendTotalPO,
} from '@self/root/src/components/OrderTotalV2/__pageObject';

import {
    mockFullCheckoutInformationWithAllCashbackOptionAllowed,
    mockFullCheckoutInformationWithEmitCashbackOptionRestricted,
    mockFullCheckoutInformationWithSpendCashbackOptionRestricted,
    mockCheckoutInformationWithValidSubscription,
    mockCheckoutInformationWithInvalidSubscription,
    mockSelectEmitCashbackOptionAction,
    mockSelectSpendCashbackOptionAction,
    mockFunctionalityByParams,
} from './mockFunctionality';
import {defaultParams} from '../../__spec__/mockData/dsbs';

const widgetPath = '@self/root/src/widgets/content/checkout/common/CheckoutSummary';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const emitAllowedTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    await mockFullCheckoutInformationWithAllCashbackOptionAllowed(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    await mockSelectEmitCashbackOptionAction();

    const title = container.querySelector(`${CashbackEmitTotalPO.root} ${CashbackEmitTotalPO.title}`);
    const value = container.querySelector(`${CashbackEmitTotalPO.root} ${CashbackEmitTotalPO.cashback}`);

    expect(title).not.toBeNull();
    expect(title.textContent).toEqual('Вернется на Плюс');

    expect(value).not.toBeNull();
    expect(value.textContent).toEqual('200');
};

export const emitRestrictedTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    await mockFullCheckoutInformationWithEmitCashbackOptionRestricted(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    await mockSelectEmitCashbackOptionAction();

    const root = container.querySelector(CashbackEmitTotalPO.root);

    expect(root).toBeNull();
};

export const spendAllowedTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    await mockFullCheckoutInformationWithAllCashbackOptionAllowed(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    await mockSelectSpendCashbackOptionAction();

    const title = container.querySelector(`${CashbackSpendTotalPO.root} ${CashbackSpendTotalPO.title}`);
    const value = container.querySelector(`${CashbackSpendTotalPO.root} ${CashbackSpendTotalPO.value}`);

    expect(title).not.toBeNull();
    expect(title.textContent).toContain('Скидка баллами Плюса');

    expect(value).not.toBeNull();
    expect(value.textContent).toEqual('-1 100 ₽');
};

export async function makeCheckForSummaryAdditionalValues(jestLayer, apiaryLayer, mandrelLayer, {
    isVisible,
    liftingType,
    price,
}) {
    await initContext(mandrelLayer);

    await jestLayer.backend.runCode(mockFunctionalityByParams, [{
        ...defaultParams,
        liftingType,
    }]);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    const additionalServiceAccordion = container.querySelector(OrderTotal.additionalServiceAccordion);

    if (!isVisible) {
        step('не должен отображаться блок доп услуг', () => expect(additionalServiceAccordion).toBeNull());
        return;
    }
    step('должен отображаться блок доп услуг', () => expect(additionalServiceAccordion).not.toBeNull());

    additionalServiceAccordion.click();
    const lifting = container.querySelector(OrderTotal.lifting);

    step('должен отображаться блок доп услуг', () => expect(lifting).not.toBeNull());
    step('должен отображаться блок доп услуг', () => expect(lifting.textContent).toContain(price));
}

export const spendRestrictedTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    await mockFullCheckoutInformationWithSpendCashbackOptionRestricted(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    await mockSelectSpendCashbackOptionAction();

    const root = container.querySelector(CashbackSpendTotalPO.root);

    expect(root).toBeNull();
};

/**
 * @expFlag all_station-subscription
 * @ticket MARKETFRONT-57855
 * @start
 */
export const hasCorrectCheckoutSummaryForValidSubscription = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);
    await mockCheckoutInformationWithValidSubscription(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    const totalPrice = container.querySelector(OrderTotal.titleSubscription);
    const summary = container.querySelector(OrderTotal.summarySubscription);
    const plusMulti = container.querySelector(OrderTotal.plusMulti);

    expect(totalPrice).toBeTruthy();
    expect(totalPrice.innerHTML).toBe('799&nbsp;₽ × 12 мес');

    expect(summary).toBeTruthy();
    expect(summary.innerHTML).toBe('799&nbsp;₽ × 12 мес');

    expect(plusMulti).toBeTruthy();
    expect(plusMulti.innerHTML).toBe('Подписка Плюс Мульти');
};

export const hasCorrectCheckoutSummaryForInvalidSubscription = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);
    await mockCheckoutInformationWithInvalidSubscription(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    const totalPrice = container.querySelector(OrderTotal.titleSubscription);
    const summary = container.querySelector(OrderTotal.summarySubscription);
    const plusMulti = container.querySelector(OrderTotal.plusMulti);

    expect(totalPrice).toBeTruthy();
    expect(totalPrice.innerHTML).toBe('9&nbsp;999&nbsp;₽/мес');

    expect(summary).toBeTruthy();
    expect(summary.innerHTML).toBe('9&nbsp;999&nbsp;₽/мес');

    expect(plusMulti).toBeTruthy();
    expect(plusMulti.innerHTML).toBe('Подписка Плюс Мульти');
};
/**
 * @expFlag all_station-subscription
 * @ticket MARKETFRONT-57855
 * @end
 */
