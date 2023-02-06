'use strict';

const chai = require('chai');
const expect = chai.expect;
const sendbernar = require('../sendbernar.js');

const messagePart = () => {
    return { mid: 'mid', hid: 'hid' };
};

const diskAttach = () => {
    return {
        path: '/path',
        name: 'filename',
        size: 65535
    };
};

const common_params = () => {
    return {
        uid: 'uid',
        caller: 'caller',
        requestId: 'reqid',
        realIp: '127.0.0.1',
        originalHost: 'mail.ru'
    };
};

const uj_params = () => {
    return {
        connectionId: 'connectionId',
        expBoxes: 'expBoxes',
        enabledExpBoxes: 'enabledExpBoxes',
        clientType: 'clientType',
        clientVersion: 'clientVersion',
        userAgent: 'userAgent',
        yandexUid: 'yandexUid',
        iCookie: 'iCookie'
    };
};

const attachesParams = () => {
    return {
        uploaded_attach_stids: [ 'stid2', 'stid1' ],
        forward_mids: [ 'mid1', 'mid2' ],
        parts_json: [ messagePart(), messagePart() ],
        disk_attaches: 'disk_attach',
        disk_attaches_json: [ diskAttach(), diskAttach() ]
    };
};

const senderParams = () => {
    return {
        from_mailbox: 'from_mailbox',
        from_name: 'from_name'
    };
};

const messageParams = () => {
    return {
        subj: 'subject',
        text: 'some text',
        html: true,
        source_mid: 'mid'
    };
};

const recipientsParams = () => {
    return {
        to: 'to@ya.ru,to2@ya.ru',
        cc: 'cc@ya.ru',
        bcc: 'bcc@ya.ru'
    };
};

const captchaParams = () => {
    return {
        captcha_entered: 'captcha_entered',
        captcha_key: 'captcha_key'
    };
};

const deliveryParams = () => {
    return {
        noanswer_remind_period: 123,
        confirm_delivery: true
    };
};

const sendMessageParams = () => {
    return {
        attaches: attachesParams(),
        sender: senderParams(),
        message: messageParams(),
        recipients: recipientsParams(),
        delivery: deliveryParams(),
        captcha: captchaParams(),
        captcha_passed: true,
        inreplyto: {
            inreplyto: 'inreplyto',
            mark_as: 'forwarded'
        },
        references: 'asdf qwer',
        lids: [ '1', '2' ],
        captcha_type: 'type',
        mentions: [ 'a@ya.ru', 'b@ya.ru' ]
    };
};

const sendShareParams = () => {
    return {
        attaches: attachesParams(),
        message: messageParams(),
        recipients: recipientsParams(),
        inreplyto: 'inreplyto',
        references: 'asdf qwer',
        admin_uid: 'uid',
        lids: [ '1', '2' ]
    };
};

const checkAttaches = (req) => {
    expect(req.body.uploaded_attach_stids).to.be.eql([ 'stid2', 'stid1' ]);
    expect(req.body.forward_mids).to.be.eql([ 'mid1', 'mid2' ]);
    expect(req.body.parts_json).to.be.eql([ JSON.stringify(messagePart()), JSON.stringify(messagePart()) ]);
    expect(req.body.disk_attaches).to.be.equal('disk_attach');
    expect(req.body.disk_attaches_json).to.be.eql([ JSON.stringify(diskAttach()), JSON.stringify(diskAttach()) ]);
};

const checkSender = (req) => {
    expect(req.body.from_mailbox).to.be.equal('from_mailbox');
    expect(req.body.from_name).to.be.equal('from_name');
};

const checkMessage = (req) => {
    expect(req.body.subj).to.be.equal('subject');
    expect(req.body.text).to.be.equal('some text');
    expect(req.body.html).to.be.equal('yes');
    expect(req.body.source_mid).to.be.equal('mid');
};

const checkRecipients = (req) => {
    expect(req.body.to).to.be.equal('to@ya.ru,to2@ya.ru');
    expect(req.body.cc).to.be.equal('cc@ya.ru');
    expect(req.body.bcc).to.be.equal('bcc@ya.ru');
};

const checkSendMessage = (req, url) => {
    expect(req.body.captcha_entered).to.be.equal('captcha_entered');
    expect(req.body.captcha_key).to.be.equal('captcha_key');
    expect(req.body.captcha_passed).to.be.equal('yes');

    expect(req.body.confirm_delivery).to.be.equal('yes');
    expect(req.body.noanswer_remind_period).to.be.equal('123');

    expect(req.body.references).to.be.equal('asdf qwer');
    expect(req.body.inreplyto).to.be.equal('inreplyto');
    expect(req.body.mark_as).to.be.equal('forwarded');
    expect(req.body.lids).to.be.eql([ '1', '2' ]);
    expect(req.body.mentions).to.be.eql([ 'a@ya.ru', 'b@ya.ru' ]);
    expect(req.path).to.be.equal(url);
};

const checkSendShare = (req, url) => {
    expect(req.body.references).to.be.equal('asdf qwer');
    expect(req.body.inreplyto).to.be.equal('inreplyto');
    expect(req.body.lids).to.be.eql([ '1', '2' ]);
    expect(req.body.admin_uid).to.be.equal('uid');
    expect(req.path).to.be.equal(url);
};

describe('Write params test', () => {
    it('should write all write_attach params', () => {
        const req = sendbernar.request.write_attachment(common_params(), uj_params(), {
            filename: 'name',
            body: 'body'
        });

        expect(req.body).to.be.equal('body');
        expect(req.path).to.be.equal('/write_attachment');
    });

    it('should write all save_draft params', () => {
        const req = sendbernar.request.save_draft(common_params(), uj_params(), {
            attaches: attachesParams(),
            sender: senderParams(),
            message: messageParams(),
            recipients: recipientsParams(),
            references: 'asdf qwer',
            inreplyto: 'inreplyto',
            lids: [ '1', '2' ]
        });

        checkAttaches(req);
        checkSender(req);
        checkMessage(req);
        checkRecipients(req);
        expect(req.body.references).to.be.equal('asdf qwer');
        expect(req.body.inreplyto).to.be.equal('inreplyto');
        expect(req.body.lids).to.be.eql([ '1', '2' ]);
        expect(req.path).to.be.equal('/save_draft');
    });

    it('should write all save_template params', () => {
        const req = sendbernar.request.save_template(common_params(), uj_params(), {
            attaches: attachesParams(),
            sender: senderParams(),
            message: messageParams(),
            recipients: recipientsParams(),
            lids: [ '1', '2' ]
        });

        checkAttaches(req);
        checkSender(req);
        checkMessage(req);
        checkRecipients(req);
        expect(req.body.lids).to.be.eql([ '1', '2' ]);
        expect(req.path).to.be.equal('/save_template');
    });

    it('should write all send_message params', () => {
        const req = sendbernar.request.send_message(common_params(), uj_params(), sendMessageParams());

        checkAttaches(req);
        checkSender(req);
        checkMessage(req);
        checkRecipients(req);
        checkSendMessage(req, '/send_message');
    });

    it('should write all send_message params', () => {
        const req = sendbernar.request.send_share(common_params(), uj_params(), sendShareParams());

        checkAttaches(req);
        checkMessage(req);
        checkRecipients(req);
        checkSendShare(req, '/send_share');
    });

    it('should write all send_delayed params', () => {
        [ [ sendbernar.request.send_delayed, '/send_delayed' ],
            [ sendbernar.request.send_undo, '/send_undo' ] ].forEach((values) => {
            const [ handler, url ] = values;

            const req = handler(common_params(), uj_params(), {
                send: sendMessageParams(),
                send_time: 228
            });

            checkAttaches(req);
            checkSender(req);
            checkMessage(req);
            checkRecipients(req);
            checkSendMessage(req, url);
        });
    });

    it('should write all cancel_send_delayed params', () => {
        [ [ sendbernar.request.cancel_send_delayed, '/cancel_send_delayed' ],
            [ sendbernar.request.cancel_send_undo, '/cancel_send_undo' ] ].forEach((values) => {
            const [ handler, url ] = values;
            const params = {
                mid: '1'
            };

            const req = handler(common_params(), uj_params(), params);

            expect(req.body.mid).to.be.equal('1');
            expect(req.path).to.be.equal(url);
        });
    });

    it('should write all list_unsuscribe params', () => {
        const req = sendbernar.request.list_unsubscribe(common_params(), uj_params(), {
            to: 'to@yandex.ru',
            subject: 'subj',
            body: 'body',
            from_mailbox: 'devnull@yandex.ru'
        });

        expect(req.body.to).to.be.equal('to@yandex.ru');
        expect(req.body.subject).to.be.equal('subj');
        expect(req.body.body).to.be.equal('body');
        expect(req.body.from_mailbox).to.be.equal('devnull@yandex.ru');
        expect(req.path).to.be.equal('/list_unsubscribe');
    });

    it('should write all limits params', () => {
        const req = sendbernar.request.limits(common_params(), uj_params(), {});
        expect(req.body).to.be.empty;

        const req2 = sendbernar.request.limits(common_params(), uj_params(), {
            to: 'to',
            cc: 'cc',
            bcc: 'bcc'
        });

        expect(req2.body.to).to.be.equal('to');
        expect(req2.body.cc).to.be.equal('cc');
        expect(req2.body.bcc).to.be.equal('bcc');
    });

    it('should write all generate_operation_id params', () => {
        const req = sendbernar.request.generate_operation_id(common_params(), uj_params());
        expect(req.body).to.be.empty;
    });

    it('should write all user_journal params', () => {
        const req = sendbernar.request.save_draft(common_params(), uj_params(), {});

        expect(req.headers.connection_id).to.be.equal('connectionId');
        expect(req.headers['X-Yandex-ExpBoxes']).to.be.equal('expBoxes');
        expect(req.headers['X-Yandex-EnabledExpBoxes']).to.be.equal('enabledExpBoxes');
        expect(req.headers['X-Yandex-ClientType']).to.be.equal('clientType');
        expect(req.headers['X-Yandex-ClientVersion']).to.be.equal('clientVersion');
        expect(req.headers.yandexuid).to.be.equal('yandexUid');
        expect(req.headers.icookie).to.be.equal('iCookie');
        expect(req.headers['User-Agent']).to.be.equal('userAgent');
    });

    it('should write all common params', () => {
        const req = sendbernar.request.save_draft(common_params(), uj_params(), {});

        expect(req.query.uid).to.be.equal('uid');
        expect(req.query.caller).to.be.equal('caller');
        expect(req.headers['X-Request-Id']).to.be.equal('reqid');
        expect(req.headers['X-Real-Ip']).to.be.equal('127.0.0.1');
        expect(req.headers['X-Original-Host']).to.be.equal('mail.ru');
    });
});
