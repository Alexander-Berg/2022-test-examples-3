const { describe, it } = require('mocha');
const { expect } = require('chai').use(require('chai-sinon'));

const { getChunksFromArgs, getFormatFromArgs, formatMessage, hash, DEFAULT_FREEZE_FORMAT } = require('./freeze-hash');

describe('freeze-hash: freezeHash', () => {
    describe('getChunksFromArgs', () => {
        const goodTypes = ['string', 'function', 'boolean', 'string'];

        // Case: freeze-hash file1.js file2.jpg file3.svg …
        it('should return array with multiple items if files exists', () => {
            const chunks = getChunksFromArgs({ files: ['a.jpg', 'a.png'] });
            expect(chunks).to.be.an('array').that.have.lengthOf(2);

            const [jpgChunk, pngChunk] = chunks;
            expect(jpgChunk, 'Chunk of a.jpg').to.be.an('array').that.have.lengthOf(4).and.include('a.jpg');
            expect(pngChunk, 'Chunk of a.png').to.be.an('array').that.have.lengthOf(4).and.include('a.png');
            expect(jpgChunk.map(v => typeof v), 'a.jpg types').to.eql(goodTypes);
            expect(pngChunk.map(v => typeof v), 'a.png types').to.eql(goodTypes);
        });

        // Case: freeze-hash -c "Custom content"
        it('should return array with one item if content exists', () => {
            const chunks = getChunksFromArgs({ content: '0', resourcePath: 'a.xxx' });
            expect(chunks).to.be.an('array').that.have.lengthOf(1);

            const [chunk] = chunks;
            expect(chunk, 'Chunk of a.xxx').to.be.an('array').that.have.lengthOf(4).and.include('a.xxx');
            expect(chunk.map(v => typeof v), 'a.xxx types').to.eql(goodTypes);
        });

        // Case: echo "Custom content" | freeze-hash
        it('should return array with one item if neither files nor content fields exists', () => {
            const chunks = getChunksFromArgs({ resourcePath: 'std.in' });
            expect(chunks).to.be.an('array').that.have.lengthOf(1);

            const [chunk] = chunks;
            expect(chunk, 'Chunk of std.in').to.be.an('array').that.have.lengthOf(4).and.include('std.in');
            expect(chunk.map(v => typeof v), 'std.in types').to.eql(goodTypes);
        });
    });

    describe('getFormatFromArgs', () => {
        it('should return default freeze format if neither format nor ext are set', () => {
            expect(getFormatFromArgs({ })).to.equal(DEFAULT_FREEZE_FORMAT);
        });

        it('should return passed format if ext is not set', () => {
            expect(getFormatFromArgs({ format: 'NOTDEFAULT' })).to.equal('NOTDEFAULT');
        });

        it('should return format without `.[ext]` if ext equal to false', () => {
            expect(getFormatFromArgs({ ext: false }))
                .to.not.equal(DEFAULT_FREEZE_FORMAT);
        });

        it('should return format without `.[ext]` if ext equal to false and format equal to default', () => {
            expect(getFormatFromArgs({ ext: false, format: DEFAULT_FREEZE_FORMAT }))
                .to.not.equal(DEFAULT_FREEZE_FORMAT);
        });

        it('should return format as is if ext equal to false but format is not equal to default', () => {
            expect(getFormatFromArgs({ ext: false, format: 'NOTDEFAULT' }))
                .to.equal('NOTDEFAULT');
        });
    });

    describe('formatMessage', () => {
        it('should format message', () => {
            expect(formatMessage('PATH', 'HASH')).to.equal('Freeze (PATH) = HASH');
        });
    });
});
