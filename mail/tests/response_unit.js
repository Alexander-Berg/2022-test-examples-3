'use strict';

const sendbernar = require('../sendbernar.js');
const chai = require('chai');
const expect = chai.expect;

describe('Unit tests sendbernar.response', () => {
    it('cases for is_internal_error', () => {
        expect(sendbernar.response.is_internal_error(500)).to.be.true;
        expect(sendbernar.response.is_internal_error(501)).to.be.true;
        expect(sendbernar.response.is_internal_error(502)).to.be.true;
        expect(sendbernar.response.is_internal_error(503)).to.be.true;
        expect(sendbernar.response.is_internal_error(599)).to.be.true;

        expect(sendbernar.response.is_internal_error(505)).to.be.false;

        expect(sendbernar.response.is_internal_error(200)).to.be.false;
        expect(sendbernar.response.is_internal_error(400)).to.be.false;
        expect(sendbernar.response.is_internal_error(401)).to.be.false;
        expect(sendbernar.response.is_internal_error(402)).to.be.false;
        expect(sendbernar.response.is_internal_error(409)).to.be.false;
    });

    it('send functions', () => {
        [ sendbernar.response.send_message,
            sendbernar.response.send_undo,
            sendbernar.response.send_delayed ].forEach((handler) => {
            expect(handler(200, '').status).to.be.equal('ok');
            expect(handler(400, '').status).to.be.equal('bad_request');
            expect(handler(401, '').status).to.be.equal('wrong_ticket');
            expect(handler(403, '').status).to.be.equal('wrong_ticket');
            expect(handler(500, '').status).to.be.equal('retryable_error');

            expect(handler(413, '').status).to.be.equal('limited');
            expect(handler(410, '').status).to.be.equal('cannot_save');

            expect(handler(505, '').status).to.be.equal('message_saved');
            expect(handler(402, '').status).to.be.equal('captcha');
            expect(handler(409, '').status).to.be.equal('spam');

            expect(() => {
                handler(123, '');
            }).to.throw(Error);
        });
    });

    it('cases for save functions', () => {
        [ sendbernar.response.save_draft,
            sendbernar.response.save_template ].forEach((handler) => {
            expect(handler(200, '').status).to.be.equal('ok');
            expect(handler(400, '').status).to.be.equal('bad_request');
            expect(handler(401, '').status).to.be.equal('wrong_ticket');
            expect(handler(403, '').status).to.be.equal('wrong_ticket');
            expect(handler(500, '').status).to.be.equal('retryable_error');

            expect(handler(413, '').status).to.be.equal('limited');
            expect(handler(410, '').status).to.be.equal('cannot_save');

            expect(() => {
                handler(123, '');
            }).to.throw(Error);
        });
    });

    it('cases for another functions', () => {
        [ sendbernar.response.write_attachment,
            sendbernar.response.list_unsubscribe,
            sendbernar.response.cancel_send_undo,
            sendbernar.response.limits,
            sendbernar.response.cancel_send_delayed,
            sendbernar.response.generate_operation_id ].forEach((handler) => {
            expect(handler(200, '').status).to.be.equal('ok');
            expect(handler(400, '').status).to.be.equal('bad_request');
            expect(handler(401, '').status).to.be.equal('wrong_ticket');
            expect(handler(403, '').status).to.be.equal('wrong_ticket');
            expect(handler(500, '').status).to.be.equal('retryable_error');

            expect(() => {
                handler(123, '');
            }).to.throw(Error);
        });
    });

    it('cases for handle_bad_request_reason', () => {
        [ 'incorrect_to',
            'incorrect_cc',
            'incorrect_bcc',
            'no_recipients',
            'max_email_addr_reached',
            'attachment_too_big' ].forEach((reason) => {
            expect(sendbernar.response.handle_bad_request_reason({ reason: reason })).to.be.equal(reason);
            expect(sendbernar.response.send_message(400, JSON.stringify({
                reason: reason
            })).reason).to.be.equal(reason);
        });

        expect(sendbernar.response.handle_bad_request_reason({ reason: '' })).to.be.undefined;
        expect(sendbernar.response.handle_bad_request_reason({ reason: undefined })).to.be.undefined;
    });

    it('cases for handle_spam_reason', () => {
        [ 'virus_found',
            'strongspam_found' ].forEach((reason) => {
            expect(sendbernar.response.handle_spam_reason({
                reason: reason
            })).to.be.equal(reason);
            expect(sendbernar.response.send_message(409, JSON.stringify({
                reason: reason
            })).reason).to.be.equal(reason);
        });

        expect(sendbernar.response.handle_spam_reason({ reason: '' })).to.be.undefined;
        expect(sendbernar.response.handle_spam_reason({ reason: undefined })).to.be.undefined;
    });
});
