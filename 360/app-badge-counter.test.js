'use strict';

const { NO_CKEY } = require('@yandex-int/duffman').flags;
const appBadgeCounterModel = require('./app-badge-counter');

let coreMock;
const serviceFn = jest.fn();
const metaFn = jest.fn();
const getAuthFn = jest.fn();
const uid = Symbol();
const metaResponse = Symbol();

beforeEach(function() {
    serviceFn.mockReset();
    metaFn.mockReset();
    getAuthFn.mockReset();

    getAuthFn.mockReturnValue({ uid });
    serviceFn.mockReturnValue(metaFn);
    metaFn.mockReturnValue(metaResponse);
    coreMock = {
        service: serviceFn,
        auth: {
            get: getAuthFn
        }
    };
});

test('should have NO_CKEY flag', () => {
    expect(appBadgeCounterModel[NO_CKEY]).toBe(true);
});

test('should return empty counters on uid mismatch', () => {
    const defaultUid = Symbol();
    const uidInModel = Symbol();
    const expectedResponse = { counters: null };

    coreMock.auth.get.mockReturnValue({ uid: defaultUid });

    expect(appBadgeCounterModel({ uid: uidInModel }, coreMock)).toEqual(expectedResponse);
});

test('should call meta/counters', () => {
    appBadgeCounterModel({ uid }, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('meta');
    expect(metaFn).toHaveBeenCalledTimes(1);
    expect(metaFn).toHaveBeenCalledWith('/counters');
});

test('should return meta response', () => {
    expect(appBadgeCounterModel({ uid }, coreMock)).toBe(metaResponse);
});
