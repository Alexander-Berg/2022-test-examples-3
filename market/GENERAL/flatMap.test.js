// eslint-disable-next-line @typescript-eslint/no-var-requires
const flatMap = require('./flatMap');

const testData = [
  {
    params: ['parameter'],
  },
];

test('flatMap 1', () => {
  const result1 = flatMap(testData, el => el.params);
  expect(result1).toHaveLength(1);
  expect(result1[0]).toBe('parameter');

  const result2 = flatMap([...testData, { params: 'parameter 2' }], el => el.params);
  expect(result2).toHaveLength(2);
  expect(result2[0]).toBe('parameter');
});
