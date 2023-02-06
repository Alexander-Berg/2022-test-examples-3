'use strict';

const expect = require('chai').expect;
const sendbernar = require('../sendbernar.js');

const common = {
    caller: 'caller',
    uid: 'uid',
    requestId: 'requestId',
    realIp: 'realIp',
    originalHost: 'originalHost'
};

const uj = {
    connectionId: 'connectionId',
    expBoxes: 'expBoxes',
    enabledExpBoxes: 'enabledExpBoxes',
    clientType: 'clientType',
    clientVersion: 'clientVersion',
    yandexUid: 'yandexUid',
    userAgent: 'userAgent',
    iCookie: 'iCookie'
};

const ujWithoutICookie = {
    connectionId: 'connectionId',
    expBoxes: 'expBoxes',
    enabledExpBoxes: 'enabledExpBoxes',
    clientType: 'clientType',
    clientVersion: 'clientVersion',
    yandexUid: 'yandexUid',
    userAgent: 'userAgent'
};

const partsJson = [ '{"mid": "mid", "hid": "hid", "rotate": "99"}', '{"mid": "mid2", "hid": "hid2", "rotate": "100"}' ];
const diskAttachesJson = [
    '{"path": "/mail?hash=asdf1234", "name": "tst.jpg", "previewPath": "/preview/preasdf1234", "size": 13666}',
    '{"path": "/mail?hash=qwerty13", "name": "mydir", "folder": "папка"}'
];

const attaches = {
    uploaded_attach_stids: [ 'stid1', 'stid2' ],
    disk_attaches: 'disk_attach',
    forward_mids: [ 'mid1', 'mid2' ],
    parts_json: partsJson,
    disk_attaches_json: diskAttachesJson
};

const sender = {
    from_mailbox: 'from_mailbox',
    from_name: 'Ginger cat'
};

const message = {
    subj: 'subject',
    text: 'hellow, world!',
    html: false,
    source_mid: 'mid',
    message_id: '<11527620055@yandex.ru>',
    current_time: 123,
    force7bit: 'yes'
};

const recipients = {
    to: [ 'to1@yandex.ru', 'to2@yandex.ru' ],
    cc: [ 'cc@yandex.ru' ],
    bcc: [ 'bcc@yandex.ru' ]
};

const delivery = {
    noanswer_remind_period: 100,
    confirm_delivery: false
};

const captcha_legacy = {
    captcha_entered: 'captcha',
    captcha_key: 'key'
};

const captcha_passed = true;

const inReplyTo = {
    inreplyto: 'message_id',
    mark_as: 'replied'
};

const operation_id = 'dead-beef-42';

describe('SendbernarResponse', () => {
    const sendMessageParams = {
        attaches: attaches,
        sender: sender,
        message: message,
        delivery: delivery,
        recipients: recipients,
        inreplyto: inReplyTo,
        lids: [ 'lid1', 'lid2' ],
        captcha: captcha_legacy,
        captcha_passed: captcha_passed,
        references: 'ref_message_id',
        mentions: [ 'a@ya.ru', 'b@ya.ru' ],
        operation_id: operation_id
    };
    it('shouldFormSendMessageParams', () => {
        expect(() => {
            sendbernar.request.send_message(common, uj, sendMessageParams);
        }).to.not.throw(Error);
    });

    it('shouldFormSendServiceParams', () => {
        expect(() => {
            sendbernar.request.send_service(common, uj, sendMessageParams);
        }).to.not.throw(Error);
    });

    it('shouldFormSendDelayedParams', () => {
        expect(() => {
            sendbernar.request.send_delayed(common, uj, {
                send_time: 111,
                send: sendMessageParams
            });
        }).to.not.throw(Error);

        expect(() => {
            sendbernar.request.send_delayed(common, uj, {
                send_time: -111,
                send: sendMessageParams
            });
        }).to.throw(Error);
    });

    const sendShareParams = {
        attaches: attaches,
        message: message,
        recipients: recipients,
        inreplyto: 'inReplyTo',
        lids: [ 'lid1', 'lid2' ],
        references: 'ref_message_id',
        admin_uid: 'uid',
    };
    it('shouldFormSendShareParams', () => {
        expect(() => {
            sendbernar.request.send_share(common, uj, sendShareParams);
        }).to.not.throw(Error);
    });

    it('shouldFormSendUndoParams', () => {
        expect(() => {
            sendbernar.request.send_undo(common, uj, {
                send_time: 111,
                send: sendMessageParams
            });
        }).to.not.throw(Error);

        expect(() => {
            sendbernar.request.send_undo(common, uj, {
                send_time: -111,
                send: sendMessageParams
            });
        }).to.throw(Error);
    });

    it('shouldFormCancelSendDelayedParams', () => {
        expect(() => {
            sendbernar.request.cancel_send_delayed(common, uj, { mid: '32' });
        }).to.not.throw(Error);
    });

    it('shouldFormCancelSendUndoParams', () => {
        expect(() => {
            sendbernar.request.cancel_send_undo(common, uj, { mid: '32' });
        }).to.not.throw(Error);
    });

    it('shouldFormSaveDraftParams', () => {
        expect(() => {
            sendbernar.request.save_draft(common, uj, {
                attaches: attaches,
                sender: sender,
                message: message,
                recipients: recipients,
                inreplyto: 'inReplyTo',
                lids: [ 'lid1', 'lid2' ],
                references: 'ref_message_id'
            });
        }).to.not.throw(Error);
    });

    it('shouldFormSaveTemplateParams', () => {
        expect(() => {
            sendbernar.request.save_template(common, uj, {
                attaches: attaches,
                sender: sender,
                message: message,
                recipients: recipients,
                lids: [ 'lid1', 'lid2' ]
            });
        }).to.not.throw(Error);
    });

    it('shouldFormWriteAttachmentParams', () => {
        expect(() => {
            sendbernar.request.write_attachment(common, uj, {
                filename: '111',
                body: {
                    any: 'thing'
                }
            });
        }).to.not.throw(Error);

        const req = sendbernar.request.write_attachment(common, uj, {
            filename: '111',
            body: 'thing'
        });

        expect(req.body).to.be.equal('thing');
        expect(req.query).to.have.property('filename').to.be.equal('111');
    });

    it('shouldFormListUnsubscribeParams', () => {
        expect(() => {
            sendbernar.request.list_unsubscribe(common, uj, {
                to: 'to@yandex.ru',
                subject: 'subject',
                body: 'body',
                from_mailbox: 'from@yandex.ru'
            });
        }).to.not.throw(Error);
    });

    it('shouldThrowAnExceptionOnUnexpectedProperty', () => {
        expect(() => {
            sendbernar.request.list_unsubscribe(common, uj, {
                to: 'to@yandex.ru',
                subject: 'subject',
                body: 'body',
                from_mailbox: 'from@yandex.ru',
                unexpected: 'property'
            });
        }).to.throw(Error);
    });

    it('shouldFormSendMessageParamsWithoutICookie', () => {
        expect(() => {
            sendbernar.request.send_message(common, ujWithoutICookie, sendMessageParams);
        }).to.not.throw(Error);
    });

    it('shouldFormSendMessageOnlyCommonParams', () => {
        expect(() => {
            sendbernar.request.send_message(common, uj, {});
        }).to.not.throw(Error);
    });
});
