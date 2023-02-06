const { describe, it, beforeEach, afterEach } = require('mocha');
const sinon = require('sinon');
const { expect } = require('chai').use(require('chai-sinon'));

const onFail = require('./on-fail');

describe('freeze-hash: onFail', () => {
    const message = 'Some important message here';
    const error = { message: 'universum', code: 42 };
    const messageRe = new RegExp(message);

    beforeEach(() => {
        sinon.stub(process, 'exit');
        sinon.stub(process.stderr, 'write');
    });

    afterEach(() => {
        process.exit.restore();
        process.stderr.write.restore();
    });

    it('.onFail should exit with code 1', () => {
        onFail(message);
        expect(process.exit).to.be.calledWith(1);
        expect(process.stderr.write).to.be.calledWith(sinon.match(messageRe));
    });

    it('.onFail should exit with user exit code', () => {
        onFail(message, error);
        expect(process.exit).to.be.calledWith(42);
        expect(process.stderr.write).to.be.calledWith(sinon.match(messageRe));
    });
});
