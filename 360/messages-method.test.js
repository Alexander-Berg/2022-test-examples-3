'use strict';

const messagesMethod = require('./messages-method.js');

test('должен бросить исключение без параметров', function() {
    expect(() => messagesMethod()).toThrow(/MESSAGES_METHOD_UNKNOWN/);
});

test('должен бросить исключение, если метод не найден', function() {
    expect(() => messagesMethod({})).toThrow(/MESSAGES_METHOD_UNKNOWN/);
});

describe('"tab" ->', function() {
    it('должен вернуть "messages_by_folder_and_tab" для "tab"', function() {
        expect(messagesMethod({ tab: 'default' })).toEqual('messages_by_folder_and_tab');
    });

    it('должен вернуть "messages_by_folder_and_tab" для "tab" + "fid', function() {
        expect(messagesMethod({ tab: 'default', fid: '2' })).toEqual('messages_by_folder_and_tab');
    });

    it('должен вернуть "threads_by_folder_and_tab" для "tab" + "threaded"', function() {
        expect(messagesMethod({ tab: 'default', threaded: true })).toEqual('threads_by_folder_and_tab');
    });

    it('должен вернуть "threads_by_folder_and_tab" для "tab" + "fid" + "threaded"', function() {
        expect(messagesMethod({ tab: 'default', fid: '2', threaded: true })).toEqual('threads_by_folder_and_tab');
    });

    it('должен вернуть "v2/messages_unread_by_tab" для "tab" + "fid" + "unread"', function() {
        expect(messagesMethod({ tab: 'default', fid: '2', unread: true })).toEqual('v2/messages_unread_by_tab');
    });

    it('должен вернуть "threads_by_folder_and_tab" для "tab" + "fid" + "threaded" + "unread"', function() {
        expect(messagesMethod({
            tab: 'default',
            fid: '2',
            threaded: true,
            unread: true
        })).toEqual('threads_by_folder_and_tab');
    });
});

describe('"fid" ->', function() {
    it('должен вернуть "threads_by_folder" для "fid" + "threaded"', function() {
        expect(messagesMethod({
            fid: '1',
            threaded: true
        })).toEqual('threads_by_folder');
    });

    it('должен вернуть "threads_in_folder_with_pins" для "fid" + "threaded" + "pinned', function() {
        expect(messagesMethod({
            fid: '1',
            threaded: true,
            pinned: true
        })).toEqual('threads_in_folder_with_pins');
    });

    it('должен вернуть "messages_by_folder" для "fid"', function() {
        expect(messagesMethod({
            fid: '1'
        })).toEqual('messages_by_folder');
    });

    it('должен вернуть "messages_in_folder_with_pins" для "fid" + "pinned"', function() {
        expect(messagesMethod({
            fid: '1',
            pinned: true
        })).toEqual('messages_in_folder_with_pins');
    });

    it('должен вернуть "messages_unread_by_folder" для fid', () => {
        expect(messagesMethod({
            fid: '1',
            unread: true
        })).toEqual('messages_unread_by_folder');
    });

    it('не должен вернуть "messages_unread_by_folder" для fid + threaded', () => {
        expect(messagesMethod({
            fid: '1',
            unread: true,
            threaded: true
        })).not.toEqual('messages_unread_by_folder');
    });
});

describe('"tid" ->', function() {
    it('должен вернуть "messages_by_thread" для "tid"', function() {
        expect(messagesMethod({
            tid: 't1'
        })).toEqual('messages_by_thread');
    });

    it('должен вернуть "messages_by_thread_with_pins" для "tid" + "pinned"', function() {
        expect(messagesMethod({
            tid: 't1',
            pinned: true
        })).toEqual('messages_by_thread_with_pins');
    });
});

describe('"lid" ->', function() {
    it('должен вернуть "messages_by_label" для "lid"', function() {
        expect(messagesMethod({
            lid: '123'
        })).toEqual('messages_by_label');
    });
});

describe('with tabs', function() {
    describe('"fid" ->', function() {
        it('должен вернуть "v2/threads_by_tab" для табного fid + "threaded"', function() {
            expect(messagesMethod({
                fid: '-10',
                threaded: true,
                withTabs: true
            })).toEqual('v2/threads_by_tab');
        });

        it('должен вернуть "v2/threads_by_tab" для табного fid + "threaded" + "unread"', function() {
            expect(messagesMethod({
                fid: '-10',
                threaded: true,
                unread: true,
                withTabs: true
            })).toEqual('v2/threads_by_tab');
        });

        it('должен вернуть "v2/threads_in_tab_with_pins" для табного fid + "threaded" + "pinned"', function() {
            expect(messagesMethod({
                fid: '-10',
                threaded: true,
                pinned: true,
                withTabs: true
            })).toEqual('v2/threads_in_tab_with_pins');
        });

        it('должен вернуть "v2/messages_by_tab" для табного fid', function() {
            expect(messagesMethod({
                fid: '-10',
                withTabs: true
            })).toEqual('v2/messages_by_tab');
        });

        it('должен вернуть "v2/messages_in_tab_with_pins" для табного fid + pinned', function() {
            expect(messagesMethod({
                fid: '-10',
                withTabs: true,
                pinned: true
            })).toEqual('v2/messages_in_tab_with_pins');
        });

        it('должен вернуть "v2/messages_unread_by_tab" для табного fid + "unread"', function() {
            expect(messagesMethod({
                fid: '-10',
                withTabs: true,
                unread: true
            })).toEqual('v2/messages_unread_by_tab');
        });

        it('должен вернуть "threads_by_folder" для нетабного fid + "threaded"', function() {
            expect(messagesMethod({
                fid: '1',
                threaded: true,
                withTabs: true
            })).toEqual('threads_by_folder');
        });

        it('должен вернуть "messages_by_folder" для нетабного fid', function() {
            expect(messagesMethod({
                fid: '1',
                withTabs: true
            })).toEqual('messages_by_folder');
        });
    });
});
