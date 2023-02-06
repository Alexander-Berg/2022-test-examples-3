'use strict';

const s = require('@ps-int/mail-lib').helpers.serializr;
const threadCountSchema = require('./thread-count.js');
const deserialize = s.deserialize.bind(s, threadCountSchema);

describe('threadCountSchema', () => {
    it('returns threadCount', () => {
        const data = { threads_info: { envelopes: [ { threadCount: 13 } ] } };
        expect(deserialize(data)).toEqual({ threadCount: 13 });
    });
    it('returns default value', () => {
        const data = { threads_info: { } };
        expect(deserialize(data)).toEqual({ threadCount: 0 });
    });
});
