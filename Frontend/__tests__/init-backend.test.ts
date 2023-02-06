import { initBackend } from '../init-backend';

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('init-backend', () => {
  beforeEach(() => {
    const originalWindow = { ...window };
    const windowSpy = jest.spyOn(global, 'window', 'get');

    windowSpy.mockImplementation(() => ({
      ...originalWindow,
      location: {
        ...originalWindow.location,
        search: '?sitekey=1000',
      },
      // @ts-expect-error
      parent: {
        addEventListener: () => { },
      },
    }));
  });

  test('should set handlers `refresh`, `validate` and `resetBackend`', () => {
    initBackend();

    for (let handler of ['refresh', 'validate', 'resetBackend']) {
      expect(window.rpcBridge.agent[handler]).toBeDefined();
    }
  });
});
