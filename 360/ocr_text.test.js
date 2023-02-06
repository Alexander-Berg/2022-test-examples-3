'use strict';

const method = require('./ocr_text.js');
const FormData = require('form-data');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const ocrMock = require('./_mocks/ocr.json');
const ocrEmptyMock = require('./_mocks/ocrEmpty.json');

let core;
let mockService;
let mockBunker;

const file = {
    originalname: 'test.jpg',
    buffer: Buffer.alloc(64),
    mimetype: 'image/jpeg'
};

const wrongFile = {
    originalname: 'test.zip',
    buffer: Buffer.alloc(64),
    mimetype: 'applization/zip'
};

beforeEach(() => {
    mockService = jest.fn();
    mockBunker = jest.fn().mockResolvedValue({});
    core = {
        params: {
            client: 'aphone',
            uuid: 'deadbeef'
        },
        config: {
            secrets: {
                ocrToken: 'TEST_OCR_TOKEN'
            }
        },
        service: (name) => name === 'bunker' ? mockBunker : mockService,
        req: {
            file
        }
    };
});

test('передает верные параметры в сервис', async () => {
    mockService.mockResolvedValueOnce({});

    await method(core);

    expect(mockService.mock.calls[0][1]).toBeInstanceOf(FormData);
});

test('happy path', async () => {
    mockService.mockResolvedValueOnce(ocrMock);

    const res = await method(core);

    expect(res).toMatchSnapshot();
});

test('неправильные параметры', async () => {
    expect.assertions(2);
    core.params.rotate = 'invalid';

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});

test('rotate берется из параметров', async () => {
    mockBunker.mockResolvedValue({ useParam: true });
    mockService.mockResolvedValue(ocrMock);
    core.params.rotate = '270';

    await method(core);

    expect(mockService.mock.calls[0][1]._streams).toContain('270');
});

test('rotate не берется из параметров', async () => {
    mockBunker.mockResolvedValue({ useParam: false });
    mockService.mockResolvedValue(ocrMock);
    core.params.rotate = '270';

    await method(core);

    expect(mockService.mock.calls[0][1]._streams).not.toContain('270');
});

test('happy path (empty text)', async () => {
    mockService.mockResolvedValueOnce(ocrEmptyMock);

    const res = await method(core);

    expect(res).toMatchSnapshot();
});

test('wrong file', async () => {
    expect.assertions(1);
    core.req.file = wrongFile;

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
    }
});

describe('ошибки', () => {
    it('сервис отвечает 400', async () => {
        expect.assertions(1);
        mockService.mockRejectedValueOnce(httpError(400));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(400);
        }
    });

    it('сервис отвечает 500', async () => {
        expect.assertions(1);
        mockService.mockRejectedValueOnce(httpError(500));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(500);
        }
    });
});
