import { advanceBy, advanceTo, clear } from 'jest-date-mock';
import { XmlHttpRequest } from '@yandex-int/messenger.utils/lib_cjs/mocks';

import {
    MIN_UNREAD_TTL,
    MIN_UNREAD_TIMEOUT,
    MAX_UNREAD_TIMEOUT,
    LOCAL_STORAGE_KEY_NAME,
} from '../consts';

import UnreadCounter from '../index';
import { getErrorTimeout, getLocalStorageTimeout } from '../helpers';

describe('unreadCounter', () => {
    let requests: XmlHttpRequest.RequestsIterator;
    let callback: jest.Mock;
    let controller: UnreadCounter;

    beforeEach(() => {
        advanceTo(new Date());

        callback = jest.fn();
        controller = new UnreadCounter({
            callback,
            url: 'test',
        });
        XmlHttpRequest.mock();
        requests = XmlHttpRequest.iterator();
        jest.useFakeTimers();
        jest.spyOn(global, 'setTimeout');
    });

    afterEach(() => {
        controller.stop();
        XmlHttpRequest.unmock();
        jest.useRealTimers();

        localStorage.removeItem(LOCAL_STORAGE_KEY_NAME);
        clear();
    });

    describe('#start', () => {
        it('должен брать данные из localStorage и делать запрос с расчитанным таймаутом', () => {
            const now = Date.now();
            const data = {
                currentTimestamp: now - 5000,
                expiredTimestamp: now + 5000,
                hasAuth: false,
                response: {
                    Ttl: MIN_UNREAD_TTL,
                    UnreadCount: 1,
                },
            };
            const timeout = getLocalStorageTimeout(data);

            localStorage.setItem(LOCAL_STORAGE_KEY_NAME, JSON.stringify(data));

            controller.start();
            expect(requests.size()).toBe(0);
            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 1, valueForChat: 0, chatCount: 0 });

            advanceBy(timeout);
            jest.advanceTimersByTime(timeout);

            requests.resolveNext({ UnreadCount: 4, LastUnreadTsMcs: 456, Ttl: MIN_UNREAD_TTL });

            expect(requests.size()).toBe(1);
            expect(callback).toBeCalledTimes(2);
            expect(callback).toBeCalledWith({ value: 4, valueForChat: 0, lastTimestamp: 456, chatCount: 0 });
        });

        it('должен игнорировать данные из localStorage и делать запрос с расчитанным таймаутом если передан ns', () => {
            const now = Date.now();
            const data = {
                currentTimestamp: now - 5000,
                expiredTimestamp: now + 5000,
                hasAuth: false,
                response: {
                    Ttl: MIN_UNREAD_TTL,
                    UnreadCount: 1,
                },
            };
            const timeout = MIN_UNREAD_TIMEOUT;

            localStorage.setItem(LOCAL_STORAGE_KEY_NAME, JSON.stringify(data));

            controller = new UnreadCounter({
                callback,
                url: 'test',
                ns: [1],
            });

            controller.start();

            requests.resolveNext({ UnreadCount: 1, Ttl: MIN_UNREAD_TTL });

            expect(requests.size()).toBe(1);
            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 1, valueForChat: 0, chatCount: 0 });

            advanceBy(timeout);
            jest.advanceTimersByTime(timeout);

            requests.resolveNext({ UnreadCount: 4, LastUnreadTsMcs: 456, Ttl: MIN_UNREAD_TTL });

            expect(requests.size()).toBe(2);
            expect(callback).toBeCalledTimes(2);
            expect(callback).toBeCalledWith({ value: 4, valueForChat: 0, lastTimestamp: 456, chatCount: 0 });
        });

        it('должен игнорировать данные из localStorage и делать запрос с расчитанным таймаутом если передан workspaceId', () => {
            const now = Date.now();
            const data = {
                currentTimestamp: now - 5000,
                expiredTimestamp: now + 5000,
                hasAuth: false,
                response: {
                    Ttl: MIN_UNREAD_TTL,
                    UnreadCount: 1,
                },
            };
            const timeout = MIN_UNREAD_TIMEOUT;

            localStorage.setItem(LOCAL_STORAGE_KEY_NAME, JSON.stringify(data));

            controller = new UnreadCounter({
                callback,
                url: 'test',
                workspaceId: 'test',
            });

            controller.start();

            requests.resolveNext({ UnreadCount: 1, Ttl: MIN_UNREAD_TTL });

            expect(requests.size()).toBe(1);
            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 1, valueForChat: 0, chatCount: 0 });

            advanceBy(timeout);
            jest.advanceTimersByTime(timeout);

            requests.resolveNext({ UnreadCount: 4, LastUnreadTsMcs: 456, Ttl: MIN_UNREAD_TTL });

            expect(requests.size()).toBe(2);
            expect(callback).toBeCalledTimes(2);
            expect(callback).toBeCalledWith({ value: 4, valueForChat: 0, lastTimestamp: 456, chatCount: 0 });
        });

        it('запрос должен совершаться с заданной периодичностью', () => {
            controller.start();

            requests.resolveNext({ UnreadCount: 2, LastUnreadTsMcs: 123, Ttl: MIN_UNREAD_TTL });

            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 2, valueForChat: 0, lastTimestamp: 123, chatCount: 0 });

            jest.advanceTimersByTime(MIN_UNREAD_TIMEOUT);

            advanceBy(MIN_UNREAD_TIMEOUT);

            requests.resolveNext({ UnreadCount: 4, LastUnreadTsMcs: 456, Ttl: MIN_UNREAD_TTL });

            expect(callback).toBeCalledTimes(2);
            expect(callback).toBeCalledWith({ value: 4, valueForChat: 0, lastTimestamp: 456, chatCount: 0 });
        });

        it('значени ChatUnreadCount должно приходить поле valueForChat', () => {
            controller.start();

            requests.resolveNext({ UnreadCount: 12, ChatUnreadCount: 10, LastUnreadTsMcs: 123, Ttl: MIN_UNREAD_TTL });

            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 12, valueForChat: 10, lastTimestamp: 123, chatCount: 0 });
        });

        it('запрос должен совершаться с увеличенной периодичностью, (onerror)', () => {
            let timeout = MIN_UNREAD_TIMEOUT;
            const status = 300;

            controller.start();

            for (let i = 0; i < 6; i++) {
                requests.errorNext(status);
                timeout = getErrorTimeout(status, timeout);

                expect(callback).toBeCalledTimes(0);
                expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), timeout);
                expect(requests.size()).toBe(i + 1);

                jest.advanceTimersByTime(timeout);
                advanceBy(timeout);
            }

            requests.resolveNext({ UnreadCount: 4, LastUnreadTsMcs: 123, Ttl: MIN_UNREAD_TTL });

            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 4, valueForChat: 0, lastTimestamp: 123, chatCount: 0 });

            expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), MIN_UNREAD_TIMEOUT);
        });

        it('запрос должен совершаться с указанной сервером периодичностью', () => {
            const TTL_WITHOUT_HISTORY = 600;
            const TTL_WITH_HISTORY = 10;
            const TIMEOUT_WITHOUT_HISTORY = TTL_WITHOUT_HISTORY * 1000;
            const TIMEOUT_WITH_HISTORY = TTL_WITH_HISTORY * 1000;

            controller.start();
            requests.resolveNext({ Ttl: TTL_WITHOUT_HISTORY });

            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 0, valueForChat: 0, chatCount: 0 });

            jest.advanceTimersByTime(TIMEOUT_WITHOUT_HISTORY);
            advanceBy(TIMEOUT_WITHOUT_HISTORY);

            requests.resolveNext({ UnreadCount: 5, LastUnreadTsMcs: 567, Ttl: TTL_WITH_HISTORY });

            jest.advanceTimersByTime(TIMEOUT_WITH_HISTORY);
            advanceBy(TIMEOUT_WITHOUT_HISTORY);

            expect(callback).toBeCalledTimes(2);
            expect(callback).toBeCalledWith({ value: 5, valueForChat: 0, lastTimestamp: 567, chatCount: 0 });
        });

        it('запрос должен совершаться с увеличенной периодичностью (bad status)', () => {
            let timeout = MIN_UNREAD_TIMEOUT;
            const status = 400;

            controller.start();

            for (let i = 0; i < 6; i++) {
                requests.resolveNext('', { status });
                timeout = getErrorTimeout(status, timeout);

                expect(callback).toBeCalledTimes(0);
                expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), timeout);
                expect(requests.size()).toBe(i + 1);

                jest.advanceTimersByTime(timeout);
                advanceBy(timeout);
            }

            requests.resolveNext({ UnreadCount: 4, LastUnreadTsMcs: 123, Ttl: MIN_UNREAD_TTL });

            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 4, valueForChat: 0, lastTimestamp: 123, chatCount: 0 });

            expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), MIN_UNREAD_TIMEOUT);
        });

        it('таймаут должен быть максимальным в случае 401 ошибки, (в ответе)', () => {
            controller.start();

            requests.resolveNext({ Status: 401 });

            expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), MAX_UNREAD_TIMEOUT);
            expect(callback).toBeCalledTimes(0);

            jest.advanceTimersByTime(MAX_UNREAD_TIMEOUT);
            advanceBy(MAX_UNREAD_TIMEOUT);

            expect(requests.size()).toBe(2);
        });

        it('Остановка запросов в случае отсутствия поля Ttl (в ответе)', () => {
            const spy = jest.spyOn(controller, 'stop');

            controller.start();

            requests.resolveNext({ });

            expect(spy).toHaveBeenCalled();

            expect(requests.size()).toBe(1);

            spy.mockRestore();
        });

        it('таймаут должен быть максимальным в случае 403 ошибки, (в ответе)', () => {
            controller.start();

            requests.resolveNext({ Status: 403 });

            expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), MAX_UNREAD_TIMEOUT);
            expect(callback).toBeCalledTimes(0);

            jest.advanceTimersByTime(MAX_UNREAD_TIMEOUT);
            advanceBy(MAX_UNREAD_TIMEOUT);

            expect(requests.size()).toBe(2);
        });

        it('таймаут должен быть максимальным в случае 401 ошибки, (bad status)', () => {
            controller.start();

            requests.resolveNext('', { status: 401 });

            expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), MAX_UNREAD_TIMEOUT);
            expect(callback).toBeCalledTimes(0);

            jest.advanceTimersByTime(MAX_UNREAD_TIMEOUT);
            advanceBy(MAX_UNREAD_TIMEOUT);

            expect(requests.size()).toBe(2);
        });

        it('таймаут должен быть максимальным в случае 403 ошибки, (bad status)', () => {
            controller.start();

            requests.resolveNext('', { status: 403 });

            expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), MAX_UNREAD_TIMEOUT);
            expect(callback).toBeCalledTimes(0);

            jest.advanceTimersByTime(MAX_UNREAD_TIMEOUT);
            advanceBy(MAX_UNREAD_TIMEOUT);

            expect(requests.size()).toBe(2);
        });
    });

    describe('#stop', () => {
        it('после вызова #stop запросы должны прекратиться', () => {
            controller.start();

            requests.resolveNext({ UnreadCount: 2, LastUnreadTsMcs: 123, Ttl: MIN_UNREAD_TTL });

            expect(callback).toBeCalledTimes(1);
            expect(callback).toBeCalledWith({ value: 2, valueForChat: 0, lastTimestamp: 123, chatCount: 0 });

            controller.stop();

            jest.advanceTimersByTime(MIN_UNREAD_TIMEOUT);
            advanceBy(MAX_UNREAD_TIMEOUT);

            expect(requests.next()).toBeFalsy();
            expect(callback).toBeCalledTimes(1);
        });
    });
});
