import { getProtoModelTitle } from './getProtoModelTitle';

describe('getProtoModelTitle', () => {
  it('works', () => {
    expect(getProtoModelTitle({})).toEqual('');
    expect(getProtoModelTitle({ titles: [{ value: 'test', isoCode: 'ru' }] })).toEqual('test');
  });
});
