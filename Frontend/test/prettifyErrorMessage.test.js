'use strict';

const sinon = require('sinon');
const { assert } = require('chai');
const prettifyErrorMessage = require('./../helpers/error-logger/prettifyErrorMessage');

sinon.assert.expose(assert, { prefix: '' });

describe('prettyfyErrorMessage', () => {
    it('Reduces the full path in the message to a relative one (starting from web4)', () => {
        let actualErrorMessage = 'Rebuild failed for "web4/.build/pages-desktop/search/all/aboba.js"';
        let prettifyError = prettifyErrorMessage(new Error('Rebuild failed for "/Users/aboba/arcadia/frontend/projects/web4/.build/pages-desktop/search/all/aboba.js"'));

        assert.equal(prettifyError.message, actualErrorMessage);
    });

    it('Must not change other paths', () => {
        let actualErrorMessage = 'Nothing to be served for /static/features/EntityFeedback@desktop.js';
        let prettifyError = prettifyErrorMessage(new Error(actualErrorMessage));

        assert.equal(prettifyError.message, actualErrorMessage);
    });

    it('Must not modify messages without paths', () => {
        let actualErrorMessage = 'Timeout awaiting \'request\' for 1000ms';
        let prettifyError = prettifyErrorMessage(new Error(actualErrorMessage));

        assert.equal(prettifyError.message, actualErrorMessage);
    });

    it('The error message can be of the string type', () => {
        let actualErrorMessage = 'Rebuild failed for "web4/.build/pages-desktop/search/all/aboba.js"';
        let prettifyError = prettifyErrorMessage('Rebuild failed for "/Users/aboba/arcadia/frontend/projects/web4/.build/pages-desktop/search/all/aboba.js"');

        assert.equal(prettifyError.message, actualErrorMessage);
    });
});
