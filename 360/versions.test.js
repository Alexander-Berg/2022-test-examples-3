'use strict';

jest.mock('../../../package.json', () => ({
    version: '1.2.3'
}));

test('возвращает версию', async () => {
    const versions = require('./versions');

    const res = await versions({});

    expect(res).toEqual({
        'mobile-api': '1.2.3'
    });
});
