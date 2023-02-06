/* global describe, it */

const yabunker = require('..');
const assert = require('assert');

const opts = {
    api: 'bunker-api-dot.yandex.net/v1',
};

describe('yabunker.cat', () => {
    it('should throw without path', () => {
        assert.throws(() => {
            yabunker(opts).cat();
        });
    });

    it('should return errors', done => {
        yabunker(opts)
            .cat('/.bunker-test/wat')
            .catch(err => {
                assert.equal(err.statusCode, 404);
                done();
            });
    });

    it('should get content', done => {
        yabunker(opts)
            .cat('/.bunker-test/api/text-node')
            .then(data => {
                assert.equal(data, 'Hello plain!');
                done();
            })
            .catch(done);
    });
});
