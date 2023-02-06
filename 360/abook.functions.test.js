'use strict';

const functions = require('./abook.functions.js');

describe('serializeText', () => {
    it('truncates and splits search request', () => {
        const request = {
            params: {
                text: 'abc def ghi jkl mno pqr stu vwx yz'
            }
        };

        expect(functions.serializeText(request)).toEqual(
            [ 'abc', 'def', 'ghi', 'jkl', 'mno', 'pqr', 'stu', 'vwx' ]
        );
    });
});
