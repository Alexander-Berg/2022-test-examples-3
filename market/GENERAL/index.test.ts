import capitalizeFirstLetter from '.';

describe('utils/capitalizeFirstLetter', () => {
    test.each`
        input              | output
        ${'test'}          | ${'Test'}
        ${'1p cabinet'}    | ${'1p cabinet'}
        ${'oneP cabinet'}  | ${'OneP cabinet'}
        ${' oneP cabinet'} | ${' oneP cabinet'}
        ${'маркет'}        | ${'Маркет'}
        ${'маркет '}       | ${'Маркет '}
        ${'{"foo":"bar"}'} | ${'{"foo":"bar"}'}
    `('$input ➡️ $output', ({input, output}) => {
        expect(capitalizeFirstLetter(input)).toBe(output);
    });

    test('пустая строка не меняется', () => {
        expect(capitalizeFirstLetter('')).toBe('');
    });

    test.each([
        null,
        undefined,
        10,
        0,
        (): string => '1',
        function printOne(): string {
            return '1';
        },
        {},
        {foo: 1, bar: 2},
        [],
        ['a', 'b', 'c', new Date(), new Error()],
    ])('исключение при нестроковых аргументах %p', input => {
        expect(() => capitalizeFirstLetter(input)).toThrow();
    });
});
