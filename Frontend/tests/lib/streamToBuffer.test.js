const assert = require('assert');
const fs = require('fs');
const catchErrorAsync = require('catch-error-async');

const streamToBuffer = require('lib/streamToBuffer');

describe('streamToBuffer', () => {
    it('success', async() => {
        const imageStream = fs.createReadStream('tests/data/shot_1296215782.png');
        const buffer = await streamToBuffer(imageStream);

        assert.equal(typeof buffer, 'object');
    });

    it('error', async() => {
        const imageStream = fs.createReadStream('/');
        const error = await catchErrorAsync(
            streamToBuffer, imageStream,
        );

        assert.equal(error.message, 'EISDIR: illegal operation on a directory, read');
    });
});
