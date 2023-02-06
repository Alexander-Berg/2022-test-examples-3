'use strict';

describe('sleep', () => {
    it('coverage', async () => {
        const sleep = require('./sleep.js');
        expect(await sleep(1)).toBeUndefined();
    });
});
