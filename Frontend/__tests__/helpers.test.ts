import { advanceTo, clear } from 'jest-date-mock';

import {
    prepareTtl,
    getErrorTimeout,
    getServiceNamespace,
    isValidLocalStorageData,
} from '../helpers';

import {
    MIN_UNREAD_TIMEOUT,
    MAX_UNREAD_TIMEOUT,
} from '../consts';

const mockedNow = Date.now();

describe('helpers', () => {
    beforeAll(() => {
        advanceTo(mockedNow);
    });

    afterAll(() => {
        clear();
    });

    describe('#prepareTtl', () => {
        it('should returns minimal ttl', () => {
            expect(prepareTtl(0)).toBe(10);
        });

        it('should returns ttl from response', () => {
            expect(prepareTtl(100)).toBe(100);
        });

        it('should returns maximal ttl from response', () => {
            expect(prepareTtl(1000000)).toBe(300);
        });
    });

    describe('#getServiceNamespace', () => {
        it('should returns namespace for Yandex.Health', () => {
            expect(getServiceNamespace(19)).toBe(5);
        });

        it('should returns undefined for Serp serviceId', () => {
            expect(getServiceNamespace(2)).toBeUndefined();
        });
    });

    describe('#isValidLocalStorageData', () => {
        it('should returns false for null', () => {
            expect(isValidLocalStorageData(null)).toBeFalsy();
        });

        it('should returns true for correct data', () => {
            expect(isValidLocalStorageData({
                currentTimestamp: mockedNow - 100,
                expiredTimestamp: mockedNow + 100,
                hasAuth: false,
                response: {
                    Status: 404,
                },
            })).toBeTruthy();
        });

        it('should returns false for expired timestamp', () => {
            expect(isValidLocalStorageData({
                currentTimestamp: mockedNow - 100,
                expiredTimestamp: mockedNow - 10,
                hasAuth: false,
                response: {
                    Status: 404,
                },
            })).toBeFalsy();
        });

        it('should returns false for auth user if user was without auth', () => {
            expect(isValidLocalStorageData({
                currentTimestamp: mockedNow - 100,
                expiredTimestamp: mockedNow + 100,
                hasAuth: true,
                response: {
                    Status: 404,
                },
            })).toBeFalsy();
        });

        it('should returns false for uncorrect currentTime', () => {
            expect(isValidLocalStorageData({
                currentTimestamp: mockedNow + 10,
                expiredTimestamp: mockedNow + 100,
                hasAuth: true,
                response: {
                    Status: 404,
                },
            })).toBeFalsy();
        });

        it('should returns false for uncorrect experedTimestamp', () => {
            expect(isValidLocalStorageData({
                currentTimestamp: mockedNow + 10,
                expiredTimestamp: mockedNow + 1e6,
                hasAuth: true,
                response: {
                    Status: 404,
                },
            })).toBeFalsy();
        });
    });

    describe('#getErrorTimeout', () => {
        it('should returns max timeout for 401 error', () => {
            expect(getErrorTimeout(401, 10000)).toBe(MAX_UNREAD_TIMEOUT);
        });

        it('should returns max timeout for 403 error', () => {
            expect(getErrorTimeout(403, 10000)).toBe(MAX_UNREAD_TIMEOUT);
        });

        it('should returns max timeout for big timestamp', () => {
            expect(getErrorTimeout(500, 100000000)).toBe(MAX_UNREAD_TIMEOUT);
        });

        it('should returns twin timeout', () => {
            expect(getErrorTimeout(500, MIN_UNREAD_TIMEOUT)).toBe(MIN_UNREAD_TIMEOUT * 2);
        });
    });
});
