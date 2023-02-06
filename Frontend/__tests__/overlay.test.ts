import OverlayContainer from '../overlay';

describe('Overlay', () => {
  test('should show and hide', () => {
    const overlay = new OverlayContainer();

    expect(overlay.isVisible()).toBeFalsy();

    overlay.show();

    expect(overlay.isVisible()).toBeTruthy();

    overlay.hide();

    expect(overlay.isVisible()).toBeFalsy();
  });

  test('destroy should remove container from parent', () => {
    document.body.innerHTML = `
      <div id="container">
      </div>
    `;

    const overlay = new OverlayContainer();
    const container = document.getElementById('container') as HTMLElement;

    container.appendChild(overlay.container);

    expect(container.children.length).toBe(1);

    overlay.show();
    overlay.destroy();

    expect(container.children.length).toBe(0);
  });

  test('should call onHide when hide function is called', () => {
    const overlay = new OverlayContainer();
    const container = document.getElementById('container') as HTMLElement;
    const mockFn = jest.fn();

    container.appendChild(overlay.container);

    overlay.onHide = mockFn;

    overlay.hide();

    expect(mockFn).not.toBeCalled();

    overlay.show();
    overlay.hide();

    expect(mockFn).toBeCalledTimes(1);
  });

  test('should be hidden for screen readers when it is closed', () => {
    const overlay = new OverlayContainer();

    const iframe = document.createElement('iframe');
    overlay.setIframe(iframe);

    expect(Number.parseInt(iframe.getAttribute('tabindex'))).toBeLessThan(0);
    expect(iframe.getAttribute('aria-hidden') === 'true').toBeTruthy();

    overlay.show();
    expect(Number.parseInt(iframe.getAttribute('tabindex'))).toBe(0);
    expect(iframe.getAttribute('aria-hidden') === 'true').toBeFalsy();

    overlay.hide();
    expect(Number.parseInt(iframe.getAttribute('tabindex'))).toBeLessThan(0);
    expect(iframe.getAttribute('aria-hidden') === 'true').toBeTruthy();
  });
});
