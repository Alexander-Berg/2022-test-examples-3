import {screen, within} from '@testing-library/dom';

import SoldOutOverlay
    from '@self/root/src/widgets/content/cart/CartList/containers/CartOfferPictureContainer/__pageObject';
import CartOfferAvailabilityInfo
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferAvailabilityInfo/__pageObject';
import {visibleStrategyId} from '@self/root/src/widgets/content/cart/__spec__/mocks/mockData';

import {FITTING} from '../components/CartOffer/constants';

const DEFAULT_AVAILABILITY_INFO = 'Осталось 2 штуки';
const widgetPath = '../';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const soldOutBadgeVisibleTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {props: {isEda: false, visibleStrategyId}});

    const soldOutBadge = container.querySelector(SoldOutOverlay.soldOutBadge);

    expect(soldOutBadge).not.toBeNull();
};

export const limitWarningVisibleTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {props: {isEda: false, visibleStrategyId}});

    const availabilityInfo = container.querySelector(CartOfferAvailabilityInfo.root);

    expect(availabilityInfo).not.toBeNull();
    expect(availabilityInfo.textContent).toBe(DEFAULT_AVAILABILITY_INFO);
};

export const fashionInfoTestCase = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);
    await apiaryLayer.mountWidget(widgetPath, {props: {isEda: false, visibleStrategyId}});

    const fittingInfo = screen.getByTestId('fittingInfo');
    const fittingIcon = within(fittingInfo).getByTestId('fittingIcon');

    expect(fittingIcon).toBeInTheDocument();
    expect(fittingInfo.textContent).toBe(FITTING);
};
