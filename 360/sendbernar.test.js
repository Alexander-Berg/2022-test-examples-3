'use strict';

const aiMock = require('../../../test/mock/ai.json');
const sMock = require('../../../test/mock/sendbernar.json');
const sendbernar = require('./sendbernar');
const status = require('./status');

const params = sMock.params;
const send = sMock.send;
const save = sMock.save;

const mockSendbernar = jest.fn();
const core = {
    params: {},
    auth: {
        get: () => aiMock
    },
    service: () => mockSendbernar
};
core.status = status(core);

afterEach(() => {
    mockSendbernar.mockReset();
});

describe('prepareParams', () => {
    describe('send_message', () => {
        it('simple message', function() {
            core.params = { ...params.base, ...params.simple };

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.simple });
        });

        it('with attachments', function() {
            core.params = { ...params.base, ...params.attach };

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.attach });
        });

        it('with inreplyto', function() {
            core.params = { ...params.base, ...params.inreplyto };

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.inreplyto });
        });

        it('forward', function() {
            core.params = { ...params.base, ...params.forward };

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.forward });
        });

        it('with delivery', function() {
            core.params = { ...params.base, ...params.delivery };

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.delivery });
        });

        it('with captcha', function() {
            core.params = Object.assign({}, params.base, params.captcha);

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.captcha });
        });

        it('with phone', function() {
            core.params = Object.assign({}, params.base, params.phone);

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.phone });
        });

        it('template', function() {
            core.params = Object.assign({}, params.base, params.template);

            sendbernar.prepareParams(core, 'send_message');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_message');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...send.base, ...send.template });
        });
    });

    describe('send_delayed', () => {
        it('simple message', function() {
            core.params = { ...params.base, ...params.simple, send_time: '2021-07-01 10:00:01' };

            sendbernar.prepareParams(core, 'send_delayed');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('send_delayed');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({
                ...send.base,
                ...send.simple,
                send_time: 1625122801
            });
        });
    });

    describe('save', () => {
        it('simple message', function() {
            core.params = Object.assign({}, params.base, params.simple);

            sendbernar.prepareParams(core, 'save');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('save_draft');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...save.base, ...save.simple });
        });

        it('with attachments', function() {
            core.params = Object.assign({}, params.base, params.attach);

            sendbernar.prepareParams(core, 'save');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('save_draft');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...save.base, ...save.attach });
        });

        it('with inreplyto', function() {
            core.params = Object.assign({}, params.base, params.inreplyto);

            sendbernar.prepareParams(core, 'save');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('save_draft');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...save.base, ...save.inreplyto });
        });

        it('template', function() {
            core.params = Object.assign({}, params.base, params.template);

            sendbernar.prepareParams(core, 'save', 'fid');

            expect(mockSendbernar.mock.calls[0][0]).toEqual('save_template');
            expect(mockSendbernar.mock.calls[0][1]).toEqual({ ...save.base, ...save.template });
        });
    });
});

describe('filter', () => {
    it('status: ok', function() {
        expect(sendbernar.filter(core, { status: 'ok' }, () => 'ok')).toEqual('ok');
    });

    it('status: error', function() {
        expect(sendbernar.filter(core, { status: 'smth wrong' }, () => 'ok'))
            .toEqual({ status: { status: 3, phrase: 'smth wrong' } });
    });

    it('status: bad_request', function() {
        expect(sendbernar.filter(core, { status: 'bad_request', reason: 'some reason' }, () => 'ok'))
            .toEqual({ status: { status: 3, phrase: 'some reason' } });
    });

    it('captcha', function() {
        const data = {
            status: 'captcha',
            reason: 'some reason',
            object: {
                captcha: {
                    key: '001AoDKM08psRUukb6JSSaVjhGXGL79k',
                    url: 'https://ext.captcha.yandex.net/image?key=001AoDKM08psRUukb6JSSaVjhGXGL79k'
                },
                stored: {
                    mid: '123',
                    fid: '6'
                }
            }
        };

        expect(sendbernar.filter(core, data, () => 'ok'))
            .toEqual({
                status: {
                    status: 3,
                    phrase: 'captcha'
                },
                captcha: {
                    key: '001AoDKM08psRUukb6JSSaVjhGXGL79k',
                    url: 'https://ext.captcha.yandex.net/image?key=001AoDKM08psRUukb6JSSaVjhGXGL79k'
                },
                stored: {
                    mid: '123',
                    fid: '6'
                }
            });
    });

    it('from_cache', function() {
        const data = {
            status: 'from_cache',
            object: { messageId: '<636591599756015@myt5-2931549b5be7.qloud-c.yandex.net>' }
        };

        expect(sendbernar.filter(core, data, () => 'ok'))
            .toEqual('ok');
    });

    it('limited', () => {
        const data = {
            status: 'limited',
            reason: 'some reason',
            object: {
                message: 'limited',
                reason: 'msg_too_big'
            }
        };

        expect(sendbernar.filter(core, data, () => 'ok'))
            .toEqual({
                status: {
                    status: 3,
                    phrase: 'limited'
                }
            });
    });

    it('no status', function() {
        expect(sendbernar.filter(core, 7, () => 'ok'))
            .toEqual({ status: { status: 3, phrase: 'PERM_FAIL' } });
    });
});

describe('error', () => {
    it('smth wrong', function() {
        expect(sendbernar.error(core, new Error()))
            .toEqual({ status: { status: 3, phrase: 'sendbernar_error' } });
    });

    it('message', function() {
        expect(sendbernar.error(core, 'smth wrong'))
            .toEqual({ status: { status: 3, phrase: 'smth wrong' } });
    });
});
