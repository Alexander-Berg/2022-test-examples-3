import { EMimeTypes, put } from 'neo/lib/ajax';
import { mockFetch, RESPONSE_OK } from 'neo/tests/mocks/fetch';

describe('put', () => {
  it('Подставляет правильный Content-Type ', async() => {
    mockFetch(RESPONSE_OK);
    const fetchSpy = jest.spyOn(window, 'fetch');
    await put('__url__', { requestContentType: 'text' });

    expect(fetchSpy).toBeCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: {
          'Content-Type': EMimeTypes.text,
        },
      }),
    );
  });

  it('Подставляет правильный Content-Type по умолчанию', async() => {
    mockFetch(RESPONSE_OK);
    const fetchSpy = jest.spyOn(window, 'fetch');
    await put('__url__');

    expect(fetchSpy).toBeCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: {
          'Content-Type': EMimeTypes.json,
        },
      }),
    );
  });
});
