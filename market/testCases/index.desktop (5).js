// flowlint-next-line untyped-import: off
import {waitFor} from '@testing-library/dom';

import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

import {widgetParams} from '../../fixtures/data';

const widgetPath = '@self/root/src/widgets/content/RootScrollBox';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const checkCashbackTextTestCase = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    text
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);
    const cashback = container.querySelector(`${CashbackInfo.root} ${Text.root}`);

    expect(cashback).not.toBeNull();
    expect(cashback.textContent).toEqual(text);
};

export const checkStyleTestCase = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    className,
    isContains = true
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);
    const cashback = container.querySelector(`${CashbackInfo.root} ${Text.root}`);

    expect(cashback).not.toBeNull();
    const classNameIndex = cashback.className.indexOf(className);

    if (isContains) {
        expect(classNameIndex).not.toEqual(-1);
    } else {
        expect(classNameIndex).toEqual(-1);
    }
};

export const checkHasCashbackTooltipTestCase = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    tooltipId,
    hasTooltip
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetParams);

    await waitFor(async () => {
        if (hasTooltip) {
            expect(container.querySelector(tooltipId)).not.toBeNull();
        } else {
            expect(container.querySelector(tooltipId)).toBeNull();
        }
    }, {
        timeout: 1000,
    });
};
