import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {visibleStrategyId} from './mockData';

const DEFAULT_ITEMS_COUNT = 'Всего: 2 товара ';
const DEFAULT_ITEMS_VALUE = '140 000 ₽';
const widgetPath = '../';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const summaryIsNotVisibleTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {isEda: false, visibleStrategyId});

    const summary = container.querySelector(OrderTotal.root);

    expect(summary).toBeNull();
};

export const summaryDisplayedCorrectTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {isEda: false, visibleStrategyId});

    const summary = container.querySelector(OrderTotal.root);
    const itemsCount = container.querySelector(OrderTotal.itemsCount);
    const itemsValue = container.querySelector(OrderTotal.itemsValue);

    expect(summary).not.toBeNull();
    expect(itemsCount.textContent).toBe(DEFAULT_ITEMS_COUNT);
    expect(itemsValue.textContent).toBe(DEFAULT_ITEMS_VALUE);
};
