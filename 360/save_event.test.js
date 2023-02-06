'use strict';

const ApiError = require('../../../routes/helpers/api-error.js');
const method = require('./save_event.js');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;
let mockRequest;

jest.mock('../_helpers/get-message-body.js', () => () => ({
    info: {
        attachments: [
            {
                hid: '1.2',
                display_name: 'event.ics',
                class: 'general',
                narod: false,
                size: 1111,
                mime_type: 'text/calendar',
                download_url: '',
                is_inline: false,
                content_id: ''
            },
            {
                hid: '1.3',
                display_name: 'image.jpg',
                class: 'image',
                narod: false,
                size: 2222,
                mime_type: 'image/jpeg',
                download_url: '',
                is_inline: false,
                content_id: ''
            }
        ]
    }
}));

beforeEach(() => {
    mockService = jest.fn();
    mockRequest = jest.fn().mockResolvedValueOnce({
        messageAttachmentUrl: 'http://example.com/event.ics'
    });

    core = {
        params: {},
        auth: {
            get: () => ({ uid: '42', locale: 'ru' })
        },
        service: () => mockService,
        request: mockRequest
    };
});

test('отвечаем 400, если нет параметров', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('happy path', async () => {
    mockService.mockResolvedValueOnce({
        eventInfo: {
            externalId: 'external42'
        }
    });
    mockService.mockResolvedValueOnce({
        id: 'internal42'
    });
    mockService.mockResolvedValueOnce({});
    core.params.mid = '42';
    core.params.uuid = 'deadbeef42';
    core.params.decision = 'maybe';

    const res = await method(core);

    expect(mockRequest.mock.calls).toMatchSnapshot();
    expect(mockService.mock.calls).toMatchSnapshot();
    expect(res).toEqual({});
});

test('no hid', async () => {
    expect.assertions(2);

    core.params.mid = '42';
    core.params.uuid = 'deadbeef42';
    core.params.decision = 'yes';
    jest.resetModules();
    jest.doMock('../_helpers/get-message-body.js', () => () => ({}));

    try {
        await require('./save_event.js')(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('ics attachment is not found');
    }
});

test('http error', async () => {
    expect.assertions(2);

    mockService.mockRejectedValueOnce(httpError(500));
    core.params.mid = '42';
    core.params.uuid = 'deadbeef42';
    core.params.decision = 'no';

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
    }
});

test('service error', async () => {
    expect.assertions(2);

    mockService.mockRejectedValueOnce({});
    core.params.mid = '42';
    core.params.uuid = 'deadbeef42';
    core.params.decision = 'no';

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
    }
});
