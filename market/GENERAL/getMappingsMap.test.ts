import { EntityMapping } from 'src/pages/ModelEditorCluster/types';
import { getMappingsMap } from 'src/pages/ModelEditorCluster/utils/getMappingsMap';
import { partialWrapper } from 'src/test/utils/partialWrapper';

describe('getMappingsMap', () => {
  it('works with minimal data', () => {
    expect(getMappingsMap(123, [], {}, [])).toEqual({ 123: [] });
  });
  it('works', () => {
    expect(
      getMappingsMap(123, [234], { 234: [1123] }, [
        partialWrapper<EntityMapping>({ internalId: 1123 }),
        partialWrapper<EntityMapping>({ internalId: 1234 }),
      ])
    ).toEqual({ 123: [{ internalId: 1234 }], 234: [{ internalId: 1123 }] });
  });
});
