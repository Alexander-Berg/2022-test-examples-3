'use strict';

const BlockStat = require('../../utils/blockstat');

describe('blockstat-helpers', () => {
    let sandbox;

    beforeEach(function() {
        sandbox = sinon.createSandbox();
        sandbox.stub(BlockStat, 'dictionary').callsFake([
            ['snippet', '254'],
            ['images', '277'],
            ['title', '82'],
            ['source', '186'],
            ['wizard', '358'],
            ['news', '3'],
            ['market', '4'],
            ['tab', '2'],
            ['ad', '1'],
            ['pos', '84'],
            ['p0', '85'],
            ['upper', '134'],
            ['pimportant', '613'],
            ['top', '77']
        ]);
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('path', function() {
        it('should return valid decoded path if encoded specified', function() {
            assert.strictEqual(BlockStat.path('254.277.82'), '/snippet/images/title');
        });

        it('should return valid decoded path if decoded specified', function() {
            assert.strictEqual(BlockStat.path('/snippet/images/title'), '/snippet/images/title');
        });

        it('should return tech path if tech path specified', function() {
            assert.strictEqual(BlockStat.path('tech'), 'tech');
        });

        it('should return tech path if tech subpath specified', function() {
            assert.strictEqual(BlockStat.path('tech.test.this'), 'tech.test.this');
        });
    });

    describe('complex', function() {
        it('should parse complex value', function() {
            assert.strictEqual(BlockStat.complex('254.277'), 'snippet/images');
        });

        it('should return string as is', function() {
            assert.strictEqual(BlockStat.complex('snippet/images'), 'snippet/images');
        });

        it('should return number as is', function() {
            assert.strictEqual(BlockStat.complex('1494500162559'), '1494500162559');
        });

        it('should return number as is if parse failed', function() {
            assert.strictEqual(BlockStat.complex('9459'), '9459');
        });
    });

    describe('vars', function() {
        it('should return valid decoded vars if encoded string specified', function() {
            assert.deepEqual(BlockStat.vars('186=358'), { source: 'wizard' });
        });

        it('should return valid decoded vars if decoded string specified', function() {
            assert.deepEqual(BlockStat.vars('source=wizard'), { source: 'wizard' });
        });

        it('should return string as is if string begins with dash', function() {
            assert.deepEqual(BlockStat.vars('-pos=1'), { '-pos': '1' });
        });

        it('should return valid decoded vars if encoded object specified', function() {
            assert.deepEqual(BlockStat.vars({ '186': '358' }), { source: 'wizard' });
        });

        it('should return valid decoded vars if decoded object specified', function() {
            assert.deepEqual(BlockStat.vars({ source: 'wizard' }), { source: 'wizard' });
        });

        it('should not decode with dictionary if object`s key starts with dash', function() {
            assert.deepEqual(BlockStat.vars({ '-source': 'wizard' }), { '-source': 'wizard' });
        });

        it('should uri decode value if object`s key starts with dash', function() {
            assert.deepEqual(
                BlockStat.vars({ '-adapters': 'news-bno-view%7Cbno%7Cbno' }),
                { '-adapters': 'news-bno-view|bno|bno' }
            );
        });

        it('should not throw if uri decoding of key value is failed', function() {
            assert.doesNotThrow(() => {
                // eslint-disable-next-line max-len
                BlockStat.vars({ '-blob': 'ZQAyAA8BABS%AcACowECABT1AsACeQMAFIIEwAKQBQMAAJcJwAcKBAAUugnAApcBBQAU5QrAAqcBBgAUoAzAAsMBBwAU9w3AAt8BCAAU6g-AAqsBCQAUqRHAAq8BCgAU7BLAArsBCwAUuxTAAr8BDAAUjhbAAr8BDQAU4RfAAqEBDgAUlhnAAokBZwAAAOgCgARoFLQBlQUDAWcgAADoAoAEaTxpeGm0AQ__' });
            });
        });
    });
});
