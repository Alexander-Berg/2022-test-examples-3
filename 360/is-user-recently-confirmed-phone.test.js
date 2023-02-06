'use strict';

const isUserRecentlyConfirmedPhone = require('./is-user-recently-confirmed-phone.js');

const now = () => Math.ceil(Date.now() / 1000);

test('works without phones', async () => {
    const request = jest.fn().mockResolvedValueOnce({ phone: [] });
    const result = await isUserRecentlyConfirmedPhone({}, { request });
    expect(result).toEqual({ check: false });
});

test('works without secure phone', async () => {
    const request = jest.fn().mockResolvedValueOnce({ phone: [ {
        secure: '0',
        confirmed: now()
    } ] });
    const result = await isUserRecentlyConfirmedPhone({}, { request });
    expect(result).toEqual({ check: false });
});

test('works with secure phone (check=true)', async () => {
    const request = jest.fn().mockResolvedValueOnce({ phone: [ {
        secure: '1',
        confirmed: now() - 10
    } ] });
    const result = await isUserRecentlyConfirmedPhone({}, { request });
    expect(result).toEqual({ check: true, remainTime: expect.any(Number) });
});

test('works with secure phone (check=false)', async () => {
    const request = jest.fn().mockResolvedValueOnce({ phone: [ {
        secure: '0',
        confirmed: now()
    }, {
        secure: '1',
        confirmed: now() - 3600
    } ] });
    const result = await isUserRecentlyConfirmedPhone({}, { request });
    expect(result).toEqual({ check: false });
});
