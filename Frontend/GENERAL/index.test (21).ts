import SchemaValidator, { InvalidSchemaError } from './index';

const schema = {
    $schema: 'http://json-schema.org/draft-07/schema',
    title: 'Test schema',
    type: 'object',
    properties: {
        field1: {
            description: 'test string field',
            type: 'string',
        },
        field2: {
            description: 'test number field',
            type: 'number',
        },
    },
    required: [
        'field1',
        'field2',
    ],
};

describe('SchemaValidator', () => {
    it('should return nothing if schema are valid', () => {
        const validator = new SchemaValidator(schema);
        const obj = { field1: 'test', field2: 2 };

        const expected = undefined;

        const actual = validator.validate(obj);

        expect(actual).toEqual(expected);
    });

    it('should throw InvalidSchemaError when schema invalid', () => {
        const validator = new SchemaValidator(schema);
        const obj = { field1: 2, field2: 'test' };

        expect(() => validator.validate(obj)).toThrow(InvalidSchemaError);
    });

    it('should produce correct error message on every call', () => {
        const validator = new SchemaValidator(schema);
        const obj1 = { field1: 'test', field2: 'test' };
        const obj2 = { field1: 2, field2: 'test' };

        const expectedMessage1 = 'failed to validate schema: data.field2 should be number';
        const expectedMessage2 = 'failed to validate schema: data.field1 should be string, data.field2 should be number';

        expect(() => validator.validate(obj1)).toThrow(expectedMessage1);
        expect(() => validator.validate(obj2)).toThrow(expectedMessage2);
        expect(() => validator.validate(obj1)).toThrow(expectedMessage1);
    });
});
