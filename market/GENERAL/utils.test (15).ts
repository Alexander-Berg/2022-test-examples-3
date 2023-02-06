import { testFn } from './utils';

describe('Testing utils', () => {
  it('should always be zero', () => {
    expect(testFn()).toEqual(0);
  });
});
