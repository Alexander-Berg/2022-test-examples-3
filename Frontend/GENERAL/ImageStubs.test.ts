import assert from 'assert';

import { defaults as stubs, ImageStubs } from 'utils/ImageStubs';

describe('ImageStubs', () => {
    it('должен возвращать Url', () => {
        const imageStubs = new ImageStubs(['a', 'b', 'c']);

        assert.strictEqual(imageStubs.nextUrl(8), 'c');
    });

    it('должен возвращать значения без повторов, пока не кончились урлы', () => {
        const imageStubs = new ImageStubs(['a', 'b', 'c']);

        assert.strictEqual(imageStubs.nextUrl(0), 'a');
        assert.strictEqual(imageStubs.nextUrl(0), 'b');
        assert.strictEqual(imageStubs.nextUrl(0), 'c');
        assert.strictEqual(imageStubs.nextUrl(), 'a');
        assert.strictEqual(imageStubs.nextUrl(), 'b');
    });

    it('должен возвращать урлы из исходного состояния, если отдал все урлы', () => {
        const imageStubs = new ImageStubs(['a', 'b', 'c']);

        assert.strictEqual(imageStubs.nextUrl(0), 'a');
        assert.strictEqual(imageStubs.nextUrl(0), 'b');
        assert.strictEqual(imageStubs.nextUrl(0), 'c');
        assert.strictEqual(imageStubs.nextUrl(0), 'a');
    });

    it('должен использовать захардкоженный список урлов, если передали пустой', () => {
        const imageStubs = new ImageStubs();

        assert.strictEqual(imageStubs.nextUrl(), stubs[0]);
        assert.strictEqual(imageStubs.nextUrl(), stubs[1]);
        assert.strictEqual(imageStubs.nextUrl(), stubs[2]);
    });
});
