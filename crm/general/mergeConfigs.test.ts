import { mergeConfigs } from './mergeConfigs';
import { FlagDependantConfig } from '../useBunker.types';

describe('mergeConfigs', () => {
  it('merges simple', () => {
    const data = [
      { fieldA: 'a', fieldB: 'b' },
      { fieldA: 'newA', fieldC: 'c' },
    ];

    expect(mergeConfigs(data as FlagDependantConfig[])).toEqual({
      fieldA: 'newA',
      fieldB: 'b',
      fieldC: 'c',
    });
  });

  it('ignores when no flag', () => {
    const data = [
      { fieldA: 'a', fieldB: 'b' },
      { fieldA: 'newA', fieldC: 'c', whenFlag: 'testFlag' },
    ];

    expect(mergeConfigs(data)).toEqual({ fieldA: 'a', fieldB: 'b' });
  });

  it('ok when empty', () => {
    const data = [];

    expect(mergeConfigs(data)).toEqual({});
  });
});
