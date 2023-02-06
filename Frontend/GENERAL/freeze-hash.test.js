const { describe, it } = require('mocha');
const { expect } = require('chai');

const { hash } = require('./freeze-hash');

describe('freeze-hash: freezeHash', () => {
    describe('hash', () => {
        it('should calculate hash with extless format for (0, null)', () => {
            expect(hash(new Buffer('0'))).to.equal('aUsFwVtQ');
        });

        it('should calculate hash with custom format for default path (0, null, [sha1:hash:base36:12].[ext])', () => {
            expect(hash(new Buffer('0'), null, '[sha1:hash:base36:12].[ext]')).to.equal('1fjmervfllti.bin');
        });

        it('should calculate hash with default format for (0, a.xxx)', () => {
            expect(hash(new Buffer('0'), 'a.xxx')).to.equal('aUsFwVtQ.xxx');
        });

        it('should calculate hash with custom format for (0, a.xxx, [sha1:hash:base36:12].[ext])', () => {
            expect(hash(new Buffer('0'), 'a.xxx', '[sha1:hash:base36:12].[ext]')).to.equal('1fjmervfllti.xxx');
        });
    });
});
