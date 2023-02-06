import {screen} from '@testing-library/dom';

export function checkButton(title) {
    const button = screen.getByTestId('button');
    expect(button).toBeVisible();
    expect(button.textContent).toEqual(title);
}

export function checkTitle(lastState, testData) {
    const title = screen.getByTestId((lastState.presetGlobal?.outletId || lastState.presetGlobal?.addressId) ?
        'delivery-info' : 'default-delivery-info');
    expect(title).toBeVisible();
    expect(title?.textContent).toContain(testData.title);
}

export function checkButtonClick(changeGlobalDelivery) {
    const button = screen.getByTestId('button');
    button.click();

    expect(changeGlobalDelivery).toHaveBeenCalled();
}
