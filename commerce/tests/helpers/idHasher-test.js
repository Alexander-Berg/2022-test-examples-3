const hasher = require('helpers/idHasher');
const { expect } = require('chai');

describe('`Decode and encode uid`', () => {
    describe('`encode`', () => {
        it('should encode uid', () => {
            const actual = hasher.encodeUserId(1234567890123);

            expect(actual).to.equal('7bX4uf9cpA7bX');
        });
    });

    describe('`decode`', () => {
        it('should decode uid', () => {
            const actual = hasher.decodeUserId('7bX4uf9cpA7bX');

            expect(actual).to.equal('1234567890123');
        });
    });
});
