const assert = require('assert');
const config = require('yandex-cfg');

const response = require('middlewares/heartbeatResponse');

describe('Heartbeat response middleware', () => {
    it('should transform check level to code', async() => {
        const dic = config.heartbeat.levelToStatusCode;

        // eslint-disable-next-line guard-for-in
        for (const level in dic) {
            const code = dic[level];
            const ctx = {};

            await response(ctx, () => Promise.resolve(level));

            assert.deepEqual(ctx, { body: { level }, status: code });
        }
    });
});
