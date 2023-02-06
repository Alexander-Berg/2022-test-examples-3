'use strict';

jest.mock('./_filters/sendbernar');
jest.mock('./_filters/special-statuses');
jest.mock('./_helpers/send-timestamp');

const model = require('./do-send-delayed');
const filter = require('./_filters/sendbernar');
const specialStatusesFilter = require('./_filters/special-statuses');
const getUnixTimestamp = require('./_helpers/send-timestamp').getUnixTimestamp;

let core;
let params;

const sendbernarService = jest.fn();

beforeEach(() => {
    core = {
        auth: {
            get: () => ({
                tz_offset: 'fake_tz_offset'
            })
        },
        service: jest.fn((name) => {
            if (name === 'sendbernar') {
                return sendbernarService;
            }
        }),
        request: jest.fn((name) => {
            if (name === 'captcha-type') {
                return 'fake_captcha_type';
            }
        })
    };

    params = {
        send_time: 'fake_send_time',
        send: {
            message: 'fake message'
        }
    };

    getUnixTimestamp.mockReturnValue('fake_unix_timestamp');
});

test('должен вызвать sendbernar с правильными параметрами', async () => {
    await model(params, core);

    expect(sendbernarService).toBeCalledWith('send_delayed', {
        send_time: 'fake_unix_timestamp',
        send: {
            message: 'fake message',
            captcha_type: 'fake_captcha_type'
        }
    });
});

test('должен выполнить фильтрацию ответа sendbernar-а', async () => {
    const sendbernarResponseMock = { data: 'data' };
    sendbernarService.mockReturnValueOnce(sendbernarResponseMock);

    await model(params, core);

    expect(filter).toBeCalledWith(sendbernarResponseMock);
    expect(specialStatusesFilter).not.toHaveBeenCalled();
});

test('выполняет фильтрацию с обработкой спец-статусов при withUpdatedUndoAndDelayedErrorHandling=yes', async () => {
    const sendbernarResponseMock = { data: 'data' };
    sendbernarService.mockReturnValueOnce(sendbernarResponseMock);

    await model({
        ...params,
        withUpdatedUndoAndDelayedErrorHandling: 'yes'
    }, core);

    expect(specialStatusesFilter).toBeCalledWith(sendbernarResponseMock, 'delayed_');
    expect(filter).not.toHaveBeenCalled();
});
