'use strict';

const method = require('./scan.js');
const FormData = require('form-data');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

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
    core = {
        service: () => mockService,
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
    mockService.mockResolvedValueOnce({
        cbirdaemon: {
            enhancedDocumentImage: [
                {
                    url: 'https://example.com/img'
                }
            ]
        }
    });

    const res = await method(core);

    expect(res).toMatchSnapshot();
});

test('no url', async () => {
    mockService.mockResolvedValueOnce({ what: { is: { going: 'on' } } });

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
