'use strict';

const sendbernar = require('../sendbernar.js');
const expect = require('chai').expect;

describe('SendbernarResponse', () => {
    it('shouldParse200Response', () => {
        expect(() => {
            sendbernar.response.send_message(200, JSON.stringify({
                messageId: 'messageId',
                limited: []
            }));
        }).to.not.throw(Error);

        expect(() => {
            sendbernar.response.send_service(200, JSON.stringify({
                messageId: 'messageId',
                limited: []
            }));
        }).to.not.throw(Error);

        expect(() => {
            sendbernar.response.send_share(200, JSON.stringify({
                messageId: 'messageId',
                limited: []
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse400Response', () => {
        expect(() => {
            sendbernar.response.send_message(400, JSON.stringify({
                category: 'messageId',
                message: 'message',
                reason: 'reason'
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse401Response', () => {
        expect(() => {
            sendbernar.response.send_message(401, JSON.stringify({
                smth: 'wrong_ticket'
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse402Response', () => {
        expect(() => {
            sendbernar.response.send_message(402, JSON.stringify({
                category: 'messageId',
                message: 'message',
                reason: 'reason',
                messageId: 'messageId',
                captcha: {
                    url: 'http://url',
                    key: '111'
                },
                stored: {
                    mid: 'mid',
                    fid: 'fid'
                }
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse409Response', () => {
        expect(() => {
            sendbernar.response.send_message(409, JSON.stringify({
                category: 'messageId',
                message: 'message',
                reason: 'reason',
                messageId: 'messageId',
                stored: {
                    mid: 'mid',
                    fid: 'fid'
                }
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse413Response', () => {
        expect(() => {
            sendbernar.response.send_message(413, JSON.stringify({
                messageId: 'messageId',
                category: 'messageId',
                message: 'message',
                reason: 'reason'
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse410Response', () => {
        expect(() => {
            sendbernar.response.send_message(410, JSON.stringify({
                category: 'messageId',
                message: 'message',
                reason: 'reason'
            }));
        }).to.not.throw(Error);
    });

    it('shouldParse505Response', () => {
        expect(() => {
            sendbernar.response.send_message(505, JSON.stringify({
                messageId: 'messageId',
                stored: {
                    mid: 'mid',
                    fid: 'fid'
                },
                limited: [],
                attachments: []
            }));
        }).to.not.throw(Error);
    });
});

describe('SendbernarResponseSpecialStatus', () => {
    it('shouldBeSpecianStatusFor400Reasons', () => {
        const makeErrResp = (r) => {
            return JSON.stringify({
                category: 'messageId',
                message: 'message',
                reason: r
            });
        };

        expect(sendbernar.response.send_message(
            400,
            makeErrResp('incorrect_to')).reason).to.equal('incorrect_to');

        expect(sendbernar.response.send_message(
            400,
            makeErrResp('incorrect_cc')).reason).to.equal('incorrect_cc');

        expect(sendbernar.response.send_message(
            400, makeErrResp('incorrect_bcc')).reason).to.equal(
            'incorrect_bcc');

        expect(sendbernar.response.send_message(
            400, makeErrResp('no_recipients')).reason).to.equal(
            'no_recipients');

        expect(sendbernar.response.send_message(
            400, makeErrResp('max_email_addr_reached')).reason).to.equal(
            'max_email_addr_reached');

        expect(sendbernar.response.send_message(
            400, makeErrResp('attachment_too_big')).reason).to.equal(
            'attachment_too_big');

        expect(sendbernar.response.send_message(
            400,
            makeErrResp('some_other_status')).reason).to.equal(undefined);
    });

    it('shouldBeSpecianStatusFor409Reasons', () => {
        const makeErrResp = (r) => {
            return JSON.stringify({
                category: 'messageId',
                message: 'message',
                reason: r,
                messageId: 'messageId',
                stored: {
                    mid: 'mid',
                    fid: 'fid'
                }
            });
        };

        const makeErrRespWithBanReason = (r, br) => {
            return JSON.stringify({
                category: 'messageId',
                message: 'message',
                banReason: br,
                reason: r,
                messageId: 'messageId',
                stored: {
                    mid: 'mid',
                    fid: 'fid'
                }
            });
        };

        expect(sendbernar.response.send_message(
            409,
            makeErrResp('virus_found')).reason).to.equal('virus_found');

        expect(sendbernar.response.send_message(
            409, makeErrResp('strongspam_found')).reason).to.equal(
            'strongspam_found');

        expect(sendbernar.response.send_message(
            409, makeErrRespWithBanReason('strongspam_found', 'urlRbl')).banReason).to.equal(
            'urlRbl');

        expect(sendbernar.response.send_message(
            409, makeErrResp('strongspam_found')).banReason).to.equal(undefined);

        expect(sendbernar.response.send_message(
            409,
            makeErrResp('some_other_status')).reason).to.equal(undefined);
    });
});
