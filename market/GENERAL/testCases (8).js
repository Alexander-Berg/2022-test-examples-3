import CheckoutCashbackControlPO
    from '@self/root/src/components/CheckoutCashback/__pageObject';
import {
    CashbackOption as CashbackOptionPO,
    CashbackOptionSelect as CashbackOptionSelectPO,
} from '@self/root/src/components/CheckoutCashback/components/CashbackOptionSelect/__pageObject';

import ConditionTextPO
    from '@self/root/src/components/CheckoutCashback/components/ConditionText/__pageObject';
import {waitFor} from '@testing-library/dom';

import {THIN_SPACE_CHAR as TSC} from '@self/root/src/constants/string';

import {
    mockSessionSyncForNonAuthUser,
    mockCheckoutWithoutCashbackBalanceAndEmitRestricted,
    mockCheckoutWithEmitSpendRestrictedWithoutReasond,
    mockCheckoutWithEmitAllowed,
    mockCheckoutWithSpendAllowed,
    mockCheckoutWithEmitRestricted,
    mockCheckoutWithSpendRestricted,
    mockCheckoutWithSpendRestrictedWhenCashbackBalanceNegative,
} from './mockFunctionality';
import {widgetParams} from './mockData';

const widgetPath = '@self/root/src/widgets/content/checkout/common/CheckoutCashbackControl';

const emitControlSelector = `${CashbackOptionSelectPO.root} div:nth-of-type(1) > ${CashbackOptionPO.root}`;
const spendControlSelector = `${CashbackOptionSelectPO.root} div:nth-of-type(2) > ${CashbackOptionPO.root}`;

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const widgetNonDisplayedForNonAuthUser = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    await mockSessionSyncForNonAuthUser(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    const root = container.querySelector(CheckoutCashbackControlPO.root);

    expect(root).toBeNull();
};

export const widgetNonDisplayedWhenNotCashbackBalanceAndEmitRestricted = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithoutCashbackBalanceAndEmitRestricted(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const root = container.querySelector(CheckoutCashbackControlPO.root);

    expect(root).toBeNull();
};

export const widgetNonDisplayedWhenEmitSpendRestrictedWithoutReasond = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithEmitSpendRestrictedWithoutReasond(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const root = container.querySelector(CheckoutCashbackControlPO.root);

    expect(root).toBeNull();
};

export const correctDisplayedEmitAllowed = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithEmitAllowed(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const emitControl = container.querySelector(emitControlSelector);

    await step('Опция Накопить отображается и имеет корректный текст', async () => {
        expect(emitControl).not.toBeNull();
        expect(emitControl.textContent).toEqual(`Получить3${TSC}663`);
    });

    await step('Опция Накопить активна', async () => {
        expect(container.querySelector(emitControlSelector).classList.contains('checked')).toBeTruthy();
    });
};

export const correctDisplayedSpendAllowed = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithSpendAllowed(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const spendControl = container.querySelector(spendControlSelector);

    await step('Опция Списать отображается и имеет корректный текст', async () => {
        expect(spendControl).not.toBeNull();
        expect(spendControl.textContent).toEqual(`Списать1${TSC}100`);
    });

    await step('Опция Списать становиться активной при нажатии', async () => {
        spendControl.click();

        await waitFor(async () => {
            expect(container.querySelector(spendControlSelector).classList.contains('checked')).toBeTruthy();
        }, {
            timeout: 1000,
        });
    });
};

export const correctDisplayedEmitRestricted = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithEmitRestricted(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const emitControl = container.querySelector(emitControlSelector);

    await step('Опция Накопить отображается и имеет корректный текст', async () => {
        expect(emitControl).not.toBeNull();
        expect(emitControl.textContent).toEqual('Не списыватьбаллы');
    });

    await step('Опция Накопить имеет корректный стиль', async () => {
        expect(emitControl.classList.contains('checked')).toBeTruthy();
    });
};

export const correctDisplayedSpendRestricted = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithSpendRestricted(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const spendControl = container.querySelector(spendControlSelector);

    await step('Опция Списать отображается и имеет корректный текст', async () => {
        expect(spendControl).not.toBeNull();
        expect(spendControl.textContent).toEqual('Списаниенедоступно');
    });

    await step('Опция Списать имеет корректный стиль', async () => {
        expect(spendControl.classList.contains('disabled')).toBeTruthy();
    });

    const conditionTextControl = container.querySelector(`${CheckoutCashbackControlPO.root} ${ConditionTextPO.root}`);

    await step('Текст с причиной недоступности опции списание отображается и корректен', async () => {
        expect(conditionTextControl).not.toBeNull();
        expect(conditionTextControl.textContent).toEqual('С выбранным способом оплаты списание баллов недоступно');
    });
};

export const correctDisplayedSpendRestrictedWhenCashbackBalanceNegative = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer
) => {
    await initContext(mandrelLayer);

    await mockCheckoutWithSpendRestrictedWhenCashbackBalanceNegative(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    const spendControl = container.querySelector(spendControlSelector);

    await step('Опция Списать отображается и имеет корректный текст', async () => {
        expect(spendControl).not.toBeNull();
        expect(spendControl.textContent).toEqual('Списаниенедоступно');
    });

    await step('Опция Списать имеет корректный стиль', async () => {
        expect(spendControl.classList.contains('disabled')).toBeTruthy();
    });
};
