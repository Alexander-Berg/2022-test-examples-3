'use strict';

const s = require('serializr');
const emailInfoSchema = require('./email-info.v1.js');
const Validator = require('../lib/validator/response-validator.js');
let validator;

beforeAll(() => {
    validator = new Validator(require.resolve('./email-info.v1.yaml'));
});

describe('fromMeta', () => {
    const deserialize = s.deserialize.bind(null, emailInfoSchema.fromMeta);

    it('copies and renames properties', () => {
        const result = deserialize({
            local: 'foo',
            domain: 'example.com',
            displayName: 'Vasya Poopkeen'
        });
        expect(result).toEqual({
            email: 'foo@example.com',
            displayName: 'Vasya Poopkeen'
        });
        validator.call(result);
    });

    it('allow empty email', () => {
        const result = deserialize({
            local: '',
            domain: '',
            displayName: 'Oops'
        });
        expect(result).toEqual({
            email: '',
            displayName: 'Oops'
        });
        validator.call(result);
    });

    it('skip email without domain', () => {
        const result = deserialize({
            local: 'local',
            domain: '',
            displayName: 'Oops'
        });
        expect(result).toEqual({
            email: '',
            displayName: 'Oops'
        });
        validator.call(result);
    });
});

describe('fromMbody', () => {
    const deserialize = s.deserialize.bind(null, emailInfoSchema.fromMbody);

    it('copies and renames properties', () => {
        const result = deserialize({
            email: 'foo@example.com',
            name: 'Vasya Poopkeen'
        });
        expect(result).toEqual({
            email: 'foo@example.com',
            displayName: 'Vasya Poopkeen'
        });
        validator.call(result);
    });
});

describe('fromCatdog', () => {
    const deserialize = s.deserialize.bind(null, emailInfoSchema.fromCatdog);

    it('copies, renames and transforms properties', () => {
        const result = deserialize({
            local: 'foo',
            domain: 'example.com',
            display_name: 'Vasya Poopkeen',
            valid: true,
            mono: 'VP',
            color: '#ffcc00',
            ava: {
                url: 'http://example.com/avatar.jpg',
                type: 'avatar'
            }
        });
        expect(result).toEqual({
            email: 'foo@example.com',
            displayName: 'Vasya Poopkeen',
            isValid: true,
            monogram: {
                letters: 'VP',
                color: '#ffcc00'
            },
            avatar: {
                url: 'http://example.com/avatar.jpg',
                type: 'avatar'
            }
        });
        validator.call(result);
    });
});
