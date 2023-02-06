/* global it, describe */

const assert = require('assert');
const objectAssign = require('object-assign');
const mailer = require('./index.js')();

describe('Setup', () => {
    it('Mailer should return something', () => {
        assert.ok(mailer);
    });

    it('Maler.send should exist', () => {
        assert.ok(mailer.send);
    });
});

describe('Send emails', () => {
    const mailStub = {
        from: 'Yandex <no-reply@yandex-team.ru>',
        to: 'yandex-mailer-tests@yandex-team.ru',
        headers: {
            'X-Yandex-Test': 'Sent from yandex-mail',
            'X-Mailer': null,
        },
        subject: 'Test report',
        html: '<h2>Hello</h2> <p>If you are read this, then the test should have passed</p>',
    };

    it('Mail should be send', done => {
        const mail = objectAssign({}, mailStub, {
            subject: 'Test case: Mail should be send',
        });

        mailer.send(mail)
            .then(info => {
                assert.ok(info);
                done();
            })
            .catch(err => {
                assert.fail(err);
                done();
            });
    });

    it('Mail should be send to local address', done => {
        const mail = objectAssign({}, mailStub, {
            subject: 'Test case: Mail should be send to local address',
            to: 'yandex-mailer-tests@yandex-team.ru',
        });

        mailer.send(mail)
            .then(info => {
                assert.ok(info);
                done();
            })
            .catch(err => {
                assert.fail(err);
                done();
            });
    });

    it('Mail should be send callback-style', done => {
        const mail = objectAssign({}, mailStub, {
            subject: 'Test case: Mail should be send callback-style',
        });

        mailer.send(mail, (err, info) => {
            if (err) {
                assert.fail(err);
                return done();
            }
            assert.ok(info);
            done();
        });
    });

    it('Mail should be send to a group', done => {
        const mail = objectAssign({}, mailStub, {
            to: ['yandex-mailer-tests@yandex-team.ru'],
            subject: 'Test case: Mail should be send to a group',
        });

        mailer.send(mail)
            .then(info => {
                assert.ok(info);
                done();
            })
            .catch(err => {
                assert.fail(err);
                done();
            });
    });

    it('Mail should be send in html via mail.message_body', done => {
        const mail = objectAssign({}, mailStub, {
            html: null,
            message_body: mailStub.html, // eslint-disable-line camelcase
            subject: 'Test case: Mail should be send in html via mail.message_body',
        });

        mailer.send(mail)
            .then(info => {
                assert.ok(info);
                done();
            })
            .catch(err => {
                assert.fail(err);
                done();
            });
    });

    it('Mail should be send in plain text', done => {
        const mail = objectAssign({}, mailStub, {
            html: null,
            text: 'Hello\nIf you are read this, then the test should have passed',
            subject: 'Test case: Mail should be send in plain text',
        });

        mailer.send(mail)
            .then(info => {
                assert.ok(info);
                done();
            })
            .catch(err => {
                assert.fail(err);
                done();
            });
    });

    it('Mail should be send in plain text via mail.message_body and mail.content_type', done => {
        const mail = objectAssign({}, mailStub, {
            html: null,
            message_body: 'Hello\nIf you are read this, then the test should have passed', // eslint-disable-line camelcase
            content_type: 'text/plain', // eslint-disable-line camelcase
            subject: 'Test case: Mail should be send in plain text via mail.message_body and mail.content_type',
        });

        mailer.send(mail)
            .then(info => {
                assert.ok(info);
                done();
            })
            .catch(err => {
                assert.fail(err);
                done();
            });
    });
});
