import { setCssVariables } from '../css-variables';

describe('setCssVariables', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="container"></div>
    `;

    // @ts-expect-error
    window.CSS = {
      supports: () => true,
    };
  });

  test('should set css variables on HTML element', () => {
    const container = document.getElementById('container') as HTMLElement;
    const properties = { '--var1': 'val1', '--var2': 'val2' };

    setCssVariables(container, properties);

    for (let [key, value] of Object.entries(properties)) {
      expect(container.style.getPropertyValue(key) === value).toBeTruthy();
    }
  });

  test('should warn when css names in the wrong format', () => {
    global.console.warn = jest.fn();

    const container = document.getElementById('container') as HTMLElement;
    const properties = { '--var1': 'val1', var2: 'val2', var3: 'val3' };

    setCssVariables(container, properties);

    expect(global.console.warn).toHaveBeenCalled();
  });
});
