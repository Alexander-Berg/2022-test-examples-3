import { InformationPopup, WRAPPER_ID } from './InformationPopup';

describe('InformationPopup Component', () => {
  it('should render the dialog and be able to close it on button click', () => {
    const onAction = jest.fn();
    document.body.innerHTML = `<div id=${WRAPPER_ID}></div>`;

    const popupPromise = InformationPopup('Display an info dialog', { onAction });
    const button = document.querySelector<HTMLButtonElement>('button');

    expect(button!.textContent).toEqual('Закрыть');

    button!.click();
    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onAction).toHaveBeenCalledWith(true);

    return expect(popupPromise).resolves.toBe(true);
  });
});
