'use strict';

jest.mock('../../../../schemas/message-body.v1.js');

const s = require('serializr');
const getMessageBodySchema = require('./get-message-body.js');
const deserialize = s.deserialize.bind(s, getMessageBodySchema);

describe('messageBody', () => {
    it('returns message body', () => {
        const result = deserialize({ foo: 1 });

        expect(result).toEqual({ messageBody: { foo: 1 } });
    });
});
