/* global describe, it */

const yabunker = require('..');
const assert = require('assert');

describe('yabunker', () => {
    it('should throw with invalid API', () => {
        assert.throws(() => {
            yabunker();
        }, /api option required/);
    });

    it('should not throw with valid API', () => {
        assert.doesNotThrow(() => {
            yabunker({
                api: 'http://bunker-api-dot.yandex.net',
            });
        });

        assert.doesNotThrow(() => {
            yabunker({
                api: 'https://bunker-api-dot.yandex.net',
            });
        });
    });
});
