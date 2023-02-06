'use strict';

const _omit = require('lodash/omit');
const convert = require('./send-message-params.js');

const diskAtts = [
    {
        name: 'name.pdf',
        url: 'https://yadi.sk/hash',
        size: 1234
    },
    {
        name: 'pic.jpg',
        url: 'https://yadi.sk/mail/hash?XXXX',
        preview_url: 'http://any.host/any/path',
        size: 1234
    },
    {
        name: 'folder',
        url: 'https://yadi.sk/mail/hash?ZZZZ',
        preview_url: null,
        size: 1234,
        folder: 'folder'
    },
    {
        name: 'whatever',
        url: null,
        size: '1234'
    }
];

const commonParams = {
    from_mailbox: 'foo@example.com',
    from_name: 'Vasya Pupkin',
    to: 'to@example.com',
    inreplyto: 'to@example.com',
    cc: 'cc@example.com',
    bcc: 'bcc@example.com',
    send: 'Body',
    subj: 'Subject',
    ttype: 'html',
    overwrite: '12345',
    att_ids: [ 'a1', 'a2' ],
    disk_att: JSON.stringify(diskAtts),
    ids: '12000',
    parts_json: 'invalid',
    lids: '31',
    references: 'ref',
    remind_period: 'remind_period',
    notify_on_send: 'yes',
    captcha_entered: 'captcha_entered',
    captcha_key: 'captcha_key',
    mark_as: 'replied',
    phone: '123-4567',
    message_id: '<12345678901234567890@qloud.mail.yandex.net>',
    current_time: 1234567
};

test('do-list-unsubscribe', () => {
    expect(convert(commonParams, 'do-list-unsubscribe')).toMatchSnapshot();
});

test('do-save-draft', () => {
    expect(convert(commonParams, 'do-save-draft')).toMatchSnapshot();
});

test('do-save-draft without current_time', () => {
    const params = { ...commonParams };
    delete params.current_time;

    jest.spyOn(Date, 'now').mockReturnValue(7654321000);
    expect(convert(params, 'do-save-draft')).toMatchSnapshot();
});

test('do-send-message', () => {
    expect(convert(commonParams, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message - with empty fields (coverage)', () => {
    const params = {
        ...commonParams,
        parts_json: '',
        overwrite: '',
        mark_as: 'trololo'
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message - inreplyto', () => {
    const params = _omit(commonParams, 'inreplyto');

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message save_symbol=template', () => {
    const params = {
        ...commonParams,
        save_symbol: 'template',
        mark_as: 'forwarded'
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message no overwrite', () => {
    const params = _omit(commonParams, 'overwrite');

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message + parts', () => {
    const params = {
        ...commonParams,
        parts: [ '1.1', '1.2' ]
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message + empty parts', () => {
    const params = {
        ..._omit(commonParams, 'parts_json'),
        parts: []
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message + invalid disk_att json', () => {
    const params = {
        ...commonParams,
        disk_att: '{{'
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message + empty parts + parts_json + empty overwrite', () => {
    const params = {
        ...commonParams,
        parts_json: '[{"mid":"111","hid":"1.3","rotate":0},{"mid":"111","hid":"1.4","rotate":1}]',
        parts: [],
        overwrite: ''
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message + parts_json', () => {
    const params = {
        ...commonParams,
        parts_json: '[{"mid":"111","hid":"1.3","rotate":0},{"mid":"111","hid":"1.4","rotate":1}]'
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-message + empty parts_json', () => {
    const params = {
        ...commonParams,
        parts_json: '[]'
    };

    expect(convert(params, 'do-send-message')).toMatchSnapshot();
});

test('do-send-delayed', () => {
    const params = { ...commonParams, send_time: 'send_time' };

    expect(convert(params, 'do-send-delayed')).toMatchSnapshot();
});

test('do-send-undo', () => {
    const params = {
        ...commonParams,
        send_time: '123',
        withUpdatedUndoAndDelayedErrorHandling: 42
    };
    expect(convert(params, 'do-send-undo')).toMatchSnapshot();
});

test('hideParamInLog', () => {
    const hideParamInLog = jest.fn();
    convert(commonParams, 'do-send-message', { hideParamInLog });
    expect(hideParamInLog).toHaveBeenCalledWith(expect.anything(), null, 'references', '[Hidden]');
    expect(hideParamInLog).toHaveBeenCalledWith(expect.anything(), null, 'disk_attaches_json', '[Hidden 4 item(s)]');
    expect(hideParamInLog).toHaveBeenCalledWith(expect.anything(), null, 'disk_att', '[Hidden]');
    expect(hideParamInLog).toHaveBeenCalledTimes(5);
});
