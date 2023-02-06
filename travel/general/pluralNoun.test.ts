import pluralNoun from './pluralNoun';

test.each`
    count | expected
    ${1}  | ${'стул'}
    ${2}  | ${'стула'}
    ${10} | ${'стульев'}
    ${21} | ${'стул'}
    ${24} | ${'стула'}
    ${28} | ${'стульев'}
`('pluralNoun returns $count $expected', ({count, expected}) => {
    const pluralizer = pluralNoun(['стул', 'стула', 'стульев']);
    expect(pluralizer(count)).toBe(expected);
});
