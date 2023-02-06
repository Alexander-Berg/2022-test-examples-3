'use strict';

const abookTop = require('./abook_top.js');

const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

const status = require('../_helpers/status');

let core;
let mockRequest;

beforeEach(() => {
    mockRequest = jest.fn();
    core = {
        params: {},
        request: mockRequest
    };
    core.status = status(core);
});

test('-> PERM_FAIL без параметра n', async () => {
    const res = await abookTop(core);

    expect(res.status.status).toEqual(3);
});

test('параметры вызова метода правильные', async () => {
    mockRequest.mockResolvedValueOnce({ contacts: [] });
    core.params.n = '42';

    await abookTop(core);

    expect(mockRequest).toHaveBeenCalledWith('get-abook-suggest/v0', {
        limit: 42,
        popular: true
    });
});

describe('-> OK', () => {
    beforeEach(() => {
        core.params.n = 10;
    });

    it('пустые результаты', async () => {
        mockRequest.mockResolvedValueOnce({ contacts: [] });

        const result = await abookTop(core);

        expect(result.status.status).toEqual(1);
        expect(result.contacts).toEqual({ count: 0, pager: {}, contact: [] });
    });

    it('есть результаты', async () => {
        mockRequest.mockResolvedValueOnce({
            contacts: [
                {
                    cid: 580,
                    name: 'Test name',
                    email: 'test@email',
                    ref: 'test ref',
                    phones: []
                }
            ]
        });

        const result = await abookTop(core);

        expect(result.status.status).toEqual(1);
        expect(result.contacts.contact).toHaveLength(1);
        expect(result.contacts).toContainKey('pager');
    });
});

describe('-> PERM_FAIL', () => {
    it('ошибка', async () => {
        core.params.n = 10;
        mockRequest.mockRejectedValueOnce(new CUSTOM_ERROR());

        const result = await abookTop(core);

        expect(result.status.status).toEqual(3);
    });
});
