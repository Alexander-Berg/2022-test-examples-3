import { EMimeTypes, post } from 'neo/lib/ajax';
import { mockFetch, RESPONSE_OK } from 'neo/tests/mocks/fetch';

describe('post', () => {
  it('Подставляет правильный Content-Type по умолчанию', async() => {
    mockFetch(RESPONSE_OK);
    const fetchSpy = jest.spyOn(window, 'fetch');
    await post('__url__');

    expect(fetchSpy).toBeCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: {
          'Content-Type': EMimeTypes.json,
          Accept: 'application/json; charset=utf-8',
        },
      }),
    );
  });
});
