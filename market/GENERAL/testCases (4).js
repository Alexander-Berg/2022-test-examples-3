import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import {visibleStrategyId} from './mockData';

const DEFAULT_CART_TITLE = 'Корзина';
const widgetPath = '../';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const includeCorrectCartTitle = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {isEda: false, visibleStrategyId});

    const cartTitle = container.querySelector(CartHeader.title);

    expect(cartTitle).not.toBeNull();
    expect(cartTitle.textContent).toBe(DEFAULT_CART_TITLE);
};
