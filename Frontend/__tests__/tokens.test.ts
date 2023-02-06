// import { createEvent } from 'effector';
// import { $tokens, updateToken } from '../tokens';

// const reset = createEvent();

// // eslint-disable-next-line mocha/no-skipped-tests
// describe.skip('tokens', () => {
//   beforeAll(() => {
//     $tokens.reset(reset);
//   });

//   afterAll(() => {
//     $tokens.off(reset);
//   });

//   beforeEach(() => reset());

//   test('each token should have name, defaultValue and value fields', () => {
//     Object.values($tokens.getState()).forEach(({ name, defaultValue, value }) => {
//       expect(typeof name).toBe('string');
//       expect(typeof defaultValue).toBe('string');
//       expect(typeof value).toBe('string');

//       expect(value).toBe(defaultValue);
//     });
//   });

//   test('should update token', () => {
//     const tokenName = Object.keys($tokens.getState())[0];

//     updateToken({ name: tokenName, value: 'test' });

//     expect($tokens.getState()[tokenName]).toBe('test');
//   });
// });

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tokens', () => {
  // eslint-disable-next-line mocha/no-skipped-tests
  test.skip('skip', () => { });
});
