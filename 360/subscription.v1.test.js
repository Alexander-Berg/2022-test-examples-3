'use strict';

const s = require('serializr');
const subscriptionSchema = require('./subscription.v1.js');
const Validator = require('../lib/validator/response-validator.js');
let validator;

beforeAll(() => {
    validator = new Validator(require.resolve('./subscription.v1.yaml'));
});

describe('subscription', () => {
    const deserialize = s.deserialize.bind(null, subscriptionSchema);

    const subscription = {
        email: 'foo@example.com',
        displayName: 'Foo!',
        types: '13,42',
        readFrequency: 0.2
    };
    const avatar = {
        url: 'AVA_URL',
        type: 'avatar'
    };
    const monogram = {
        mono: 'MG',
        color: '#000'
    };

    it('deserializes', () => {
        const result = deserialize({ ...subscription, foo: 'blah' });
        expect(result).toMatchSnapshot();
        validator.call(result);
    });

    it('deserializes with avatar', () => {
        const result = deserialize({ ...subscription, avatar });
        expect(result).toMatchSnapshot();
        validator.call(result);
    });

    it('deserializes with monogram', () => {
        const result = deserialize({ ...subscription, monogram });
        expect(result).toMatchSnapshot();
        validator.call(result);
    });

    it('deserializes with last received data', () => {
        const r1 = deserialize({ ...subscription, last_received_date: 123 });
        const r2 = deserialize({ ...subscription, last_received_date: '456' });
        const r3 = deserialize({ ...subscription, last_received_date: 'boo' });
        expect(r1).toMatchSnapshot();
        expect(r2).toMatchSnapshot();
        expect(r3).toMatchSnapshot();
        validator.call(r1);
        validator.call(r2);
        validator.call(r3);
    });
});
