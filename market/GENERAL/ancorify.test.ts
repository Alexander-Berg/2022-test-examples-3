import { ancorify } from 'src/shared/common-logs/helpers/ancorify';

describe('ancorify', () => {
  it('works with strings', () => {
    expect(ancorify('test1\ntest2')).toEqual('test1,test2');
  });
});
