import { hash } from 'lib/hash';

describe('hash', function () {
    it('should create hash with empty string', function () {
        expect(hash('')).toMatchInlineSnapshot(`"$da39"`);
    });

    it('should create hash with default length', function () {
        expect(hash('hello world!')).toMatchInlineSnapshot(`"$430c"`);
    });

    it('should create hash with custom length', function () {
        expect(hash('hello world!', 7)).toMatchInlineSnapshot(`"$430ce34"`);
    });
});
