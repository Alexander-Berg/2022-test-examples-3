'use strict';

jest.unmock('@yandex-int/duffman');
const model = require('./account-information.js');

const aiMock = require('./__mocks__/ai');

let core;

beforeEach(() => {
    core = {
        auth: {
            get: () => aiMock
        },
        ckey: {
            renew: jest.fn().mockReturnValue('new')
        }
    };
});

describe('models-touch/account-information', () => {
    it('должна обновить ckey', async () => {
        const result = await model({}, core);

        expect(core.ckey.renew).toBeCalled();
        expect(result.ckey).toEqual('new');
    });

    it('фильтрует ответ', async () => {
        const result = await model({}, core);

        expect(core.ckey.renew).toBeCalled();
        expect(result).toMatchSnapshot();
    });
});
