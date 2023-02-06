import { encode, decode } from '../src';

describe('decode', () => {
    const examples = [
        {
            name: 'single property',
            example: 'example',
        },
        {
            name: 'plain object',
            test: 'testDate',
            number: 33143,
            boolean: true,
        },
        {
            name: 'nested object',
            domain: {
                rules: ['rule1', 'rule2'],
                payload: { cart: { something: true } },
            },
        },
    ];

    examples.forEach((example) => {
        const { name } = example;
        const encoded = encode(example);
        const decoded = decode(encoded);

        test(`${name}`, () => {
            expect(decoded).toMatchObject(example);
        });
    });
});
