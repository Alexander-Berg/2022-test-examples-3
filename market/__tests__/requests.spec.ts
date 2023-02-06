/**
 * @jest-environment jsdom
 */

import {retryableRequest} from '../requests';

describe('retryableRequest', () => {
    it('just should work', async () => {
        await expect(retryableRequest(() => Promise.resolve('foo'))).resolves.toBe('foo');
    });

    it('should trow exception', async () => {
        await expect(retryableRequest(() => Promise.reject('foo'))).rejects.toBe('foo');
    });

    it('call passed async function 3 times before reject', async () => {
        const callback = jest.fn();

        await expect(
            retryableRequest(() => {
                callback();

                return Promise.reject('foo');
            })
        ).rejects.toBe('foo');

        expect(callback).toHaveBeenCalledTimes(3);
    });
});
