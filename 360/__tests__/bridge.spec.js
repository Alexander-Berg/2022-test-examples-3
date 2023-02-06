import {makeWebAppVisible} from '../bridge';

describe('makeWebAppVisible', () => {
  let windowSpy;

  beforeEach(() => {
    windowSpy = jest.spyOn(window, 'window', 'get');
  });

  afterEach(() => {
    windowSpy.mockRestore();
  });

  test('android', () => {
    const makeWebAppVisibleFn = jest.fn();

    windowSpy.mockImplementation(() => ({
      Android: {
        makeWebAppVisible: makeWebAppVisibleFn
      }
    }));

    makeWebAppVisible();

    expect(makeWebAppVisibleFn).toBeCalledTimes(1);
  });
});
