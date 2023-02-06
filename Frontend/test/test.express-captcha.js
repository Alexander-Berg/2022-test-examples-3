/*global describe, it, beforeEach, afterEach*/
'use strict';

var sinon = require('sinon'),
    Promise = require('bluebird');

var expect = require('chai')
    .use(require('sinon-chai'))
    .expect;

describe('express-captcha', function() {
    var CaptchaStub,
        expressCaptcha;

    beforeEach(function() {
        require('../lib/captcha');
        CaptchaStub = sinon.stub(require.cache[require.resolve('../lib/captcha')], 'exports');
        expressCaptcha = require('../index');
    });

    afterEach(function() {
        CaptchaStub.restore();
        delete require.cache[require.resolve('../index')];
    });

    describe('generate', function() {
        it('accepts optional `options` argument and returns function which accepts 3 arguments', function() {
            var generate = expressCaptcha.generate();
            expect(generate.length).to.eql(3);
        });

        it('uses `options.type` to change captcha view', function(done) {
            var options = {
                type: 'pewpew',
            };

            CaptchaStub.returns({
                generate: function(type) {
                    return new Promise(function(resolve, reject) {
                        resolve({ type: type });
                    });
                },
            });

            var generate = expressCaptcha.generate(options);
            var req = {};
            generate(req, {}, function() {
                try {
                    expect(req).to.deep.equal({
                        captcha: {
                            type: options.type,
                        },
                    });
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });

        it('sets req.captcha to null if generate rejects', function(done) {
            CaptchaStub.returns({
                generate: function() {
                    return new Promise(function(resolve, reject) {
                        reject();
                    });
                },
            });

            var generate = expressCaptcha.generate();
            var req = {};

            generate(req, {}, function() {
                try {
                    expect(req).to.have.property('captcha', null);
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });

        it('detects captcha type using req.langdetect', function(done) {
            CaptchaStub.returns({
                generate: function(type) {
                    return new Promise(function(resolve, reject) {
                        resolve({ type: type });
                    });
                },
            });

            var generate = expressCaptcha.generate();
            var req = {
                langdetect: {
                    id: 'ru',
                },
            };

            generate(req, {}, function() {
                try {
                    expect(req).to.have.deep.property('captcha.type', 'std');
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });

        it('uses estd type if no req.langdetect nor type passed', function(done) {
            CaptchaStub.returns({
                generate: function(type) {
                    return new Promise(function(resolve, reject) {
                        resolve({ type: type });
                    });
                },
            });

            var generate = expressCaptcha.generate();
            var req = {};

            generate(req, {}, function() {
                try {
                    expect(req).to.have.deep.property('captcha.type', 'estd');
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });
    });

    describe('check', function() {
        it('accepts optional `options` argument and returns function which accepts 3 arguments', function() {
            var generate = expressCaptcha.check();
            expect(generate.length).to.eql(3);
        });

        it('gets arguments from req using `options.requestParameter`', function(done) {
            CaptchaStub.returns({
                check: function(params) {
                    if (params.pewpew !== 123) {
                        return new Promise(function(resolve, reject) {
                            reject(params);
                        });
                    }
                    return new Promise(function(resolve, reject) {
                        resolve(params);
                    });
                },
            });

            var options = {
                requestParameter: 'pewpew',
            };

            var check = expressCaptcha.check(options);
            var req = {
                pewpew: {
                    pewpew: 123,
                },
            };

            check(req, {}, function() {
                try {
                    expect(req).to.have.deep.property('captcha.error', null);
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });

        it('gets arguments from req.query if request method is GET', function(done) {
            CaptchaStub.returns({
                check: function(params) {
                    if (params.pewpew !== 123) {
                        return new Promise(function(resolve, reject) {
                            reject(params);
                        });
                    }
                    return new Promise(function(resolve, reject) {
                        resolve(params);
                    });
                },
            });

            var check = expressCaptcha.check();
            var req = {
                query: {
                    pewpew: 123,
                },
                method: 'GET',
            };

            check(req, {}, function() {
                try {
                    expect(req).to.have.deep.property('captcha.error', null);
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });

        it('gets arguments from req.body if request method is POST', function(done) {
            CaptchaStub.returns({
                check: function(params) {
                    if (params.pewpew !== 123) {
                        return new Promise(function(resolve, reject) {
                            reject(params);
                        });
                    }
                    return new Promise(function(resolve, reject) {
                        resolve(params);
                    });
                },
            });

            var check = expressCaptcha.check();
            var req = {
                body: {
                    pewpew: 123,
                },
                method: 'POST',
            };

            check(req, {}, function() {
                try {
                    expect(req).to.have.deep.property('captcha.error', null);
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });

        it('returns error message if no arguments passed', function(done) {
            var options = {
                requestParameter: 'pewpew',
            };

            var check = expressCaptcha.check(options);
            var req = {};

            check(req, {}, function() {
                try {
                    expect(req.captcha.error).to.match(/is not defined/);
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });
    });
});
