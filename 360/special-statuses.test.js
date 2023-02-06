'use strict';

const specialStatusesFilter = require('./special-statuses');

test('правильно обрабатывает status=ok', () => {
    const result = specialStatusesFilter({
        status: 'ok',
        object: {
            stored: { mid: '171981210770211783', fid: '5' },
            attachments: [],
            limited: [],
            messageId: '<529871583839881@vla5-ef135f5a718d.qloud-c.yandex.net>'
        }
    }, 'fake_prefix_');

    expect(result).toEqual(expect.objectContaining({
        status: 'ok'
    }));
});

test('правильно обрабатывает status=cannot_save', () => {
    const result = specialStatusesFilter({
        status: 'cannot_save',
        object: undefined
    }, 'fake_prefix_');

    expect(result).toEqual(expect.objectContaining({
        status: 'fake_prefix_cannot_save',
        error: 'fake_prefix_cannot_save'
    }));
});

test('правильно обрабатывает status=message_saved', () => {
    const result = specialStatusesFilter({
        status: 'message_saved',
        object: {
            stored: { mid: '171981210770211783', fid: '5' },
            attachments: [],
            limited: [],
            messageId: '<529871583839881@vla5-ef135f5a718d.qloud-c.yandex.net>'
        }
    }, 'fake_prefix_');

    expect(result).toEqual(expect.objectContaining({
        status: 'fake_prefix_message_saved',
        error: 'fake_prefix_message_saved'
    }));
});
