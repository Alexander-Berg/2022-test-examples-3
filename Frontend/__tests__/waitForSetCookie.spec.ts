import * as Cookies from 'js-cookie';
import { waitForSetCookie } from '../waitForSetCookie';
jest.mock('js-cookie');

describe('waitForSetCookie', () => {
    beforeEach(() => {
        // @ts-ignore-line
        Cookies.get = jest.fn();
    });

    it('реджектится по истечении таймаута', () => {
        return expect(waitForSetCookie('q', 300)).rejects
            .toThrow('Cookie "q" is not exist');
    });

    it('резолвится со значением куки, если она есть', () => {
        const value = 'my-cookie-value';
        // @ts-ignore-line
        Cookies.get.mockReturnValue(value);

        const promise = expect(waitForSetCookie('q', 300)).resolves.toBe(value);
        expect(Cookies.get).toBeCalledTimes(1);
        return promise;
    });

    it('проверяет наличие куки и резолвится со значением, когда она появляется', () => {
        const value = 'my-cookie-value';
        // @ts-ignore-line
        setTimeout(() => Cookies.get.mockReturnValue(value), 500);

        return waitForSetCookie('q', 10000)
            .then(cookieValue => {
                expect(cookieValue).toBe(value);
                expect(Cookies.get).toBeCalledTimes(3);
            });
    });
});
