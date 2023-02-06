import { act, renderHook } from '@testing-library/react-hooks';

import { useSmartCaptcha } from '../useSmartCaptcha';

jest.mock('../useSmartCaptchaLoader');

describe('useSmartCaptcha', () => {
  let ref: { current: HTMLDivElement };

  beforeAll(() => {
    const div = document.createElement('div');
    document.body.appendChild(div);

    ref = {
      current: div,
    };
  });

  // eslint-disable-next-line mocha/no-skipped-tests
  test.skip('should render captcha and return widgetId', async () => {
    const { result, waitForNextUpdate } = renderHook(() =>
      useSmartCaptcha({ sitekey: 'mock' }, ref),
    );

    expect(result.current.widgetId).toBeUndefined();

    act(() => result.current.render());

    await waitForNextUpdate();

    expect(result.current.widgetId).toBe(0);
  });
});
