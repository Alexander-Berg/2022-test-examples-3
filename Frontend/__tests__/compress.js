console.log(''); // https://github.com/facebook/jest/issues/5792#issuecomment-376678248

const fs = require('mock-fs');
const compress = require('../compress');

describe('compress', () => {
    afterEach(() => {
        fs.restore();
    });
    it('should throw on invalid type', () => {
        fs({
            'foo.js': "console.log('foo')",
        });
        expect(() => compress('./foo.js', '$FOO$', {})).toThrow();
    });
});
