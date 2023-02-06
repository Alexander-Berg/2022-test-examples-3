import { fetchWithRetry } from '../fetch';

describe('fetchWithRetry', () => {
    const retryDelay = () => 0;
    const response = {
        ok: true,
        status: 200,
        json: () => Promise.resolve({}),
    };

    let fetch: jest.Mock;

    beforeEach(() => {
        fetch = jest.fn().mockImplementation()
            .mockResolvedValue(response);

        global.fetch = fetch;
    });

    it('Повторяет запрос в случае ошибки', async() => {
        fetch.mockImplementation()
            .mockRejectedValue(new Error('Test error'));

        expect.assertions(3);

        await fetchWithRetry('http://yandex.ru/turbo', { retryCount: 2, retryDelay })
            .catch(e => {
                expect(e.message).toEqual('Test error');
                expect(e.retryCount).toEqual(2);
                expect(fetch).toBeCalledTimes(3);
            });
    });

    it('Возвращает результат, если повторный запрос прошёл успешно', async() => {
        fetch.mockImplementation()
            .mockRejectedValueOnce(new Error('Test error'))
            .mockResolvedValueOnce(response);

        expect.assertions(2);

        await fetchWithRetry('http://yandex.ru/turbo', { retryCount: 2, retryDelay })
            .then(res => {
                expect(res).toMatchObject({ ok: true });
                expect(fetch).toBeCalledTimes(2);
            });
    });
});
