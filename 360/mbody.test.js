'use strict';

const mbodyMock = require('../../../test/mock/mbody.json');
const mbodyMock2 = require('../../../test/mock/mbody2.json');
const mbodyDiskMock = require('../../../test/mock/disk/mbody.json');
const mbodyWithAttachMock = require('../../../test/mock/mbody-with-attach.json');
const mbodyWithInlineAttachMock = require('../../../test/mock/mbody-with-inline-attach.json');

const status = require('./status.js');

let core;
let normalizeResponse;

beforeEach(() => {
    core = {
        status: status(),
        params: {}
    };
    normalizeResponse = require('./mbody.js')(core);
});

test('если была ошибка в запросе, то возвращает ошибку', () => {
    expect(normalizeResponse({
        mid: '1',
        uid: '1',
        response: {
            error: {
                error: 'wrong mid'
            }
        }
    })).toEqual({
        status: {
            status: 3,
            phrase: 'PERM_FAIL wrong mid'
        }
    });
});

test('ошибка с несуществующим mid', () => {
    expect(normalizeResponse({
        mid: '1',
        uid: '1',
        response: {
            error: {
                error: {
                    result: 'internal error',
                    error: 'exception: error in forming message: getMessageAccessParams error: unknown mid=1'
                }
            }
        }
    })).toEqual({
        status: {
            status: 3,
            phrase: 'PERM_FAIL message_body: unknown mid'
        }
    });
});

test('если что-то пошло не так, то возвращает PERM_FAIL', () => {
    expect(normalizeResponse({
        mid: '163536961468896256',
        uid: '328665045',
        response: {
            info: {}
        }
    })).toEqual({
        status: {
            status: 3,
            phrase: 'PERM_FAIL message_body: 5000'
        }
    });
});

test('okresult', () => {
    expect(normalizeResponse(mbodyMock)).toMatchSnapshot();
});

test('кейс с аттачем в bodies', () => {
    const result = normalizeResponse(mbodyWithAttachMock);

    expect(result.body).toMatchSnapshot();
});

test('еще кейс', () => {
    expect(normalizeResponse(mbodyMock2)).toMatchSnapshot();
});

test('кейс с дисковой папкой', () => {
    const result = normalizeResponse({
        mid: '1',
        uid: '1',
        response: mbodyDiskMock
    });

    expect(result).toMatchSnapshot();
});

test('кейс с инлайн аттачами (должны стать неинлайновым)', () => {
    const result = normalizeResponse(mbodyWithInlineAttachMock);

    expect(result.info.attachments).toMatchSnapshot();
});
