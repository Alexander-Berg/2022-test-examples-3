'use strict';

const s = require('serializr');
const subscriptionSchema = require('./newsletter.v1.js');
const Validator = require('../lib/validator/response-validator.js');
let validator;

beforeAll(() => {
    validator = new Validator(require.resolve('./newsletter.v1.yaml'));
});

describe('subscription', () => {
    const deserialize = s.deserialize.bind(null, subscriptionSchema);

    const subscription = {
        email: 'foo@example.com',
        displayName: 'Foo!',
        type: '13',
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
});
