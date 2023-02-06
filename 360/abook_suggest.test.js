'use strict';

const abookSuggest = require('./abook_suggest.js');

const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

const status = require('../_helpers/status');
const ai = require('../../../test/mock/ai.json');

let core;
let mockRequest;

beforeEach(() => {
    mockRequest = jest.fn();
    core = {
        params: {},
        config: {},
        auth: {
            get: jest.fn().mockReturnValue(ai)
        },
        request: mockRequest
    };
    core.status = status(core);
});

test('отдает PERM_FAIL без параметра query', async () => {
    const res = await abookSuggest(core);

    expect(res.status.status).toEqual(3);
});

describe('параметры вызова метода правильные', () => {
    it('обычный кейс, mixin из конфига', async () => {
        mockRequest.mockResolvedValueOnce({ contacts: [] });
        core.params.query = 'vasya';

        await abookSuggest(core);

        expect(mockRequest).toHaveBeenCalledWith('get-abook-suggest/v0', {
            query: 'vasya',
            limit: 10
        });
    });
});

describe('-> OK', () => {
    beforeEach(() => {
        core.params.query = 'a';
    });

    it('есть результаты', async () => {
        mockRequest.mockResolvedValueOnce({
            contacts: [
                {
                    cid: 16,
                    name: 'alexey.novarov',
                    email: 'alexey.novarov@example.com',
                    ref: 'ref1',
                    phones: []
                },
                {
                    cid: 12,
                    name: 'Karim Amanov',
                    email: 'karimamanov@example.com',
                    ref: 'ref2',
                    phones: []
                }
            ]
        });
        const result = await abookSuggest(core);

        expect(result.status.status).toEqual(1);
        expect(result.contacts.contacts).toHaveLength(2);
        expect(result.contacts).toContainAllKeys([ 'rev', 'groups', 'contacts' ]);
    });
});

describe('-> PERM_FAIL', () => {
    it('ошибка', async () => {
        mockRequest.mockRejectedValueOnce(new CUSTOM_ERROR());
        const result = await abookSuggest(core);

        expect(result.status.status).toEqual(3);
    });
});
