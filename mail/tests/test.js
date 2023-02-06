var expect = require('chai').expect;
var vdirect_module = require('vdirect');


const UID = "1";
const URL = "http://ya.ru";
const NOTSTRING = 1;

var vdirect = vdirect_module.Vdirect("keys.test");

describe('Vdirect', function() {
    it('shouldCreateModuleWithNew', function () {
        expect(function() { var v = new vdirect_module.Vdirect("keys.test");}).to.not.throw(Error);
    });
    it('shouldCreateModuleWithoutNew', function () {
        expect(function() { var v = vdirect_module.Vdirect("keys.test");}).to.not.throw(Error);
    });

    it('shouldThrowAnExceptionIfIncorrectlyFormattedFileWerePassed', function () {
        expect(function() { var v = vdirect_module.Vdirect("keys.test.invalid");}).to.throw(Error);
    });
});

describe('Vdirect.createHashForUidLink', function() {
    it('shouldAcceptUidAndLinkArguments', function() {
        expect(vdirect.createHashForUidLink(UID, URL)).to.not.be.empty;
    });

    it('shouldThrowTypeErrorOnWrongNumberOnParams', function() {
        expect(function() { vdirect.createHashForUidLink(UID, URL, "1234")}).to.throw(TypeError);
        expect(function() { vdirect.createHashForUidLink(UID)             }).to.throw(TypeError);
    });

    it('shouldThrowTypeErrorOnUndefinedIsPassedInstedOfString', function() {
        expect(function() { vdirect.createHashForUidLink(undefined, URL)}).to.throw(TypeError);
        expect(function() { vdirect.createHashForUidLink(UID, undefined)}).to.throw(TypeError);
    });

    it('shouldThrowAnExceptionWhenNonstringParamIsPassed', function() {
        expect(function() { vdirect.createHashForUidLink(NOTSTRING, URL)}).to.throw(TypeError);
        expect(function() { vdirect.createHashForUidLink(UID, NOTSTRING)}).to.throw(TypeError);
    });
});

describe('Vdirect.validateHashForUidLink', function() {
    it('shouldReturnTrueOnValidHash', function() {
        const hash = vdirect.createHashForUidLink(UID, URL);
        expect(vdirect.validateHashForUidLink(UID, URL, hash)).to.be.true;
    });

    it('shouldReturnFalseOnGarbageInsteadOnHash', function() {
        const wrong_hash = URL+UID;
        expect(vdirect.validateHashForUidLink(UID, URL, wrong_hash)).to.be.false;
    });

    it('shouldReturnFalseOnOnHashForDifferentUlr', function() {
        const wrong_hash = vdirect.createHashForUidLink(UID, URL+URL);
        expect(vdirect.validateHashForUidLink(UID, URL, wrong_hash)).to.be.false;
    });

    it('shouldReturnFalseOnOnHashForDifferentUid', function() {
        const wrong_hash = vdirect.createHashForUidLink(UID+UID, URL);
        expect(vdirect.validateHashForUidLink(UID, URL, wrong_hash)).to.be.false;
    });

    it('shouldThrowTypeErrorOnWrongNumberOnParams', function() {
        expect(function() { vdirect.validateHashForUidLink(UID, URL, "1234", "foo")}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForUidLink(UID, URL)               }).to.throw(TypeError);
    });

    it('shouldThrowTypeErrorOnUndefinedIsPassedInstedOfString', function() {
        expect(function() { vdirect.validateHashForUidLink(undefined, URL, "1234")}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForUidLink(UID, undefined, "1234")}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForUidLink(UID, URL, undefined   )}).to.throw(TypeError);
    });

    it('shouldThrowTypeErrorWhenNonstringParamIsPassed', function() {
        expect(function() { vdirect.validateHashForUidLink(NOTSTRING, URL, "1234")}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForUidLink(UID, NOTSTRING, "1234")}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForUidLink(UID, URL, NOTSTRING   )}).to.throw(TypeError);
    });
});

describe('Vdirect.validateHashForSmsLink', function() {
    const SMS_TTL = 10;

    it('shouldReturnFalseOnStrangeHash', function() {
        expect(vdirect.validateHashForSmsLink(URL, "a,sms_hash,Ran-y7u_VX6JlPPYIlL1jw", SMS_TTL)).to.be.false;
    });

    it('shouldThrowTypeErrorOnWrongNumberOnParams', function() {
        expect(function() { vdirect.validateHashForSmsLink(URL, "1234", SMS_TTL, "foo")}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForSmsLink(URL, "1234"                )}).to.throw(TypeError);
    });

    it('shouldThrowTypeErrorOnUndefinedIsPassedInstedOfString', function() {
        expect(function() { vdirect.validateHashForSmsLink(undefined, "1234", SMS_TTL)}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForSmsLink(URL, undefined,    SMS_TTL)}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForSmsLink(URL, "1234",     undefined)}).to.throw(TypeError);
    });

    it('shouldThrowTypeErrorWhenNonstringParamIsPassed', function() {
        expect(function() { vdirect.validateHashForSmsLink(NOTSTRING, URL, SMS_TTL)}).to.throw(TypeError);
        expect(function() { vdirect.validateHashForSmsLink(URL, NOTSTRING, SMS_TTL)}).to.throw(TypeError);
    });

    it('shouldThrowTypeErrorWhenNonnumberParamIsPassed', function() {
        expect(function() { vdirect.validateHashForSmsLink(URL, "1234", "1234")}).to.throw(TypeError);
    });
});
