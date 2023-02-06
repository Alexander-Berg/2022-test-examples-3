/* global describe, it */

let yabunker = require('..');
let assert = require('assert');

let opts = {
    api: 'bunker-api-dot.yandex.net/v1',
};

describe('yabunker.tree', () => {
    it('should throw without path', () => {
        assert.throws(() => {
            yabunker(opts).tree();
        });
    });

    // TODO: Restore test
    it.skip('should return error', done => {
        yabunker(opts)
            .tree('/.bunker-test/wat')
            .catch(err => {
                assert.ok(/Unexpected token/.test(err.message));
                done();
            });
    });

    // TODO: Restore test
    it.skip('should get array with nodes', done => {
        yabunker(opts)
            .tree('/.bunker-test/api')
            .then(data => {
                assert.ok(data instanceof Array);
                assert.equal(data.length, 14);
                done();
            })
            .catch(done);
    });
});
