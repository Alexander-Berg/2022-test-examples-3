/* global describe, it */

const yabunker = require('..');
const assert = require('assert');

const opts = {
    api: 'bunker-api-dot.yandex.net/v1',
};

describe('yabunker.ls', () => {
    it('should throw without path', () => {
        assert.throws(() => {
            yabunker(opts).ls();
        });
    });

    // TODO: Restore test
    it.skip('should return error', done => {
        yabunker(opts)
            .ls('/.bunker-test/wat')
            .catch(err => {
                assert.ok(/Unexpected token/.test(err.message));
                done();
            });
    });

    // TODO: Restore test
    it.skip('should get array with nodes', function(done) {
        yabunker(opts)
            .ls('/.bunker-test/api')
            .then(data => {
                assert.ok(data instanceof Array);
                assert.equal(data.length, 7);
                done();
            })
            .catch(done);
    });
});
