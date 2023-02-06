/*global describe, it, beforeEach, afterEach*/
'use strict';

var sinon = require('sinon');

var expect = require('chai')
    .use(require('sinon-chai'))
    .expect;

function fakeGenerateXml(protocol, vtype) {
    protocol = protocol ? (protocol + ':') : protocol;
    var voice = vtype ? ' voiceurl=\'' + protocol + '//i.captcha.yandex.net/voice?key=KEY\'' +
        ' voiceintrourl=\'' + protocol + '//i.captcha.yandex.net/static/intro-ru.mp3\'' : '';

    return '<?xml version="1.0"?>\n<number url=\'' + protocol + '//i.captcha.yandex.net/image?key=KEY\'' +
        voice + '>KEY</number>';
}

function fakeCheckXml(isOk) {
    return isOk ? '<image_check>ok</image_check>' : '<image_check>failed</image_check>';
}

describe('Captcha', function() {
    var gotStub,
        Captcha;

    beforeEach(function() {
        require('got');
        gotStub = sinon.stub(require.cache[require.resolve('got')], 'exports');
        Captcha = require('../lib/captcha');
    });

    afterEach(function() {
        gotStub.restore();
        delete require.cache[require.resolve('../lib/captcha')];
    });

    it('has optional options argument with optional fields', function() {
        var captcha = new Captcha();
        expect(captcha).to.have.property('apiHost', 'api.captcha.yandex.net');
        expect(captcha).to.have.property('protocol', 'any');
    });

    it('accepts options', function() {
        var captcha = new Captcha({
            apiHost: 'localhost',
            protocol: 'https',
        });
        expect(captcha).to.have.property('apiHost', 'localhost');
        expect(captcha).to.have.property('protocol', 'https');
    });

    describe('.generate(type)', function() {
        it('uses `apiHost` instance property, uses `estd` as default value of `type`', function(done) {
            var options = {
                apiHost: 'localhost',
            };

            // eslint-disable-next-line no-console
            console.log(fakeGenerateXml(''));
            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=any&checks=1')
                .callsArgWith(1, null, fakeGenerateXml(''));

            new Captcha(options).generate().then(function() {
                done();
            }, done);
        });

        it('translates `https` protocol to https=on query argument', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'https',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=on&checks=1')
                .callsArgWith(1, null, fakeGenerateXml('https'));

            new Captcha(options).generate().then(function() {
                done();
            }, done);
        });

        it('translates `http` protocol to https=off query argument', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'http',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=off&checks=1')
                .callsArgWith(1, null, fakeGenerateXml('http'));

            new Captcha(options).generate().then(function() {
                done();
            }, done);
        });

        it('translates `any` protocol to https=any query argument', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'any',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=any&checks=1')
                .callsArgWith(1, null, fakeGenerateXml(''));

            new Captcha(options).generate().then(function() {
                done();
            }, done);
        });

        it('passes `type` argument to captcha API', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'any',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=rus&https=any&checks=1')
                .callsArgWith(1, null, fakeGenerateXml(''));

            new Captcha(options).generate('rus').then(function() {
                done();
            }, done);
        });

        it('returns Promise, which resolves to object with `src` and `key` fields', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'any',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=any&checks=1')
                .callsArgWith(1, null, fakeGenerateXml(''));

            new Captcha(options).generate().then(function(object) {
                expect(object).to.deep.equal({
                    src: '//i.captcha.yandex.net/image?key=KEY',
                    key: 'KEY',
                });
            })
                .then(done, done);
        });

        it('rejects Promise with response object, if API returns bad value', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'any',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=any&checks=1')
                .callsArgWith(1, null, '');

            new Captcha(options).generate().then(function() {
                done(new Error('Should reject'));
            }, function(value) {
                expect(value).to.deep.equal('');
            })
                .then(done, done);
        });

        it('passes `vtype` argument to Captcha API', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'any',
                vtype: 'ru',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=std&https=any&checks=1&vtype=ru')
                .callsArgWith(1, null, fakeGenerateXml('', options.vtype));

            new Captcha(options).generate('std', options.vtype).then(function() {
                done();
            }, done);
        });
    });

    describe('.check(params)', function() {
        it('rejects if `params` is not an object', function(done) {
            var options = {
                apiHost: 'localhost',
                protocol: 'any',
            };

            new Captcha(options).check(void 0).then(function() {
                done(new Error('Should reject'));
            }, function(value) {
                expect(value).to.eql('no parameters passed');
                done();
            });
        });

        it('passes `params.key` and `params.rep` API and resolves with `"ok"`', function(done) {
            var options = {
                apiHost: 'localhost',
            };

            var check = {
                key: 'key',
                rep: 'rep',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/check?key=key&rep=rep')
                .callsArgWith(1, null, fakeCheckXml(true));

            new Captcha(options).check(check).then(function(value) {
                expect(value).to.eql('ok');
            })
                .then(done, done);
        });

        it('rejects if API returns bad result', function(done) {
            var options = {
                apiHost: 'localhost',
            };

            var check = {
                key: 'key',
                rep: 'rep',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/check?key=key&rep=rep')
                .callsArgWith(1, null, '');

            new Captcha(options).check(check).then(function() {
                done(new Error('Should reject'));
            }, function(value) {
                expect(value).to.eql('failed');
            })
                .then(done, done);
        });

        it('rejects if number of checks exceeded', function(done) {
            var options = {
                apiHost: 'localhost',
                checks: 2,
            };

            var check = {
                key: 'key',
                rep: 'rep',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=any&checks=' + options.checks)
                .callsArgWith(1, null, fakeGenerateXml(''));

            var captcha = new Captcha(options);

            captcha.generate().then(function() {
                var counter = 0;

                for (var i = 0; i < options.checks; i++) {
                    (function(i) {
                        gotStub
                            .withArgs('http://' + options.apiHost + '/check?key=' + check.key + '&rep=' + check.rep)
                            .callsArgWith(1, null, fakeCheckXml(true));

                        captcha.check(check).then(function(res) {
                            if (res === 'ok') {
                                counter++;
                            }

                            if (i === options.checks - 1) {
                                expect(counter).to.eql(options.checks);
                                done();
                            }
                        }, function(err) {
                            done(new Error(err));
                        });
                    })(i);
                }
            }, done);
        });

        it('accepts if processed number of checks less then or equals total', function(done) {
            var options = {
                apiHost: 'localhost',
                checks: 2,
            };

            var check = {
                key: 'key',
                rep: 'rep',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/generate?type=estd&https=any&checks=' + options.checks)
                .callsArgWith(1, null, fakeGenerateXml(''));

            var captcha = new Captcha(options);

            captcha.generate().then(function() {
                var counter = 0;

                for (var i = 0; i < options.checks + 1; i++) {
                    (function(i) {
                        gotStub
                            .withArgs('http://' + options.apiHost + '/check?key=' + check.key + '&rep=' + check.rep)
                            .callsArgWith(1, null, fakeCheckXml(i < options.checks));

                        captcha.check(check).then(function(res) {
                            if (res === 'ok') {
                                counter++;
                            }

                            if (i === options.checks) {
                                done(new Error('Status ' + res + ' when number of checks exceeded.'));
                            }
                        }, function() {
                            expect(counter).to.be.gte(options.checks);
                            done();
                        });
                    })(i);
                }
            }, done);
        });

        it('rejects when check was failed due to network connection reasons', function(done) {
            var options = {
                apiHost: 'localhost',
            };

            var check = {
                key: 'key',
                rep: 'rep',
            };

            gotStub
                .withArgs('http://' + options.apiHost + '/check?key=' + check.key + '&rep=' + check.rep)
                .callsArgWith(1, 'ECONNRESET', null);

            new Captcha(options).check(check).then(function(res) {
                done(new Error('Status ' + res + ' when request was failed.'));
            }, function(value) {
                expect(value).to.eql('ECONNRESET');
            }).then(done, done);
        });
    });
});
