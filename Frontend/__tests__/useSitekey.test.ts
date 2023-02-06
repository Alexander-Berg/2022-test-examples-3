import { useSitekey } from '../useSitekey';

describe('useSitekey', () => {
  test('should return sitekey from current location', () => {
    const originalWindow = { ...window };
    const windowSpy = jest.spyOn(global, 'window', 'get');

    // @ts-expect-error
    windowSpy.mockImplementation(() => ({
      ...originalWindow,
      location: {
        ...originalWindow.location,
        search: '?sitekey=1000',
      },
    }));

    expect(useSitekey()).toBe('1000');
  });
});
