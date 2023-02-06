import { getTitleInstruction } from 'src/shared/common-logs/helpers/getTitleInstruction';

describe('getTitleInstruction', () => {
  it('works with invalid data', () => {
    expect(getTitleInstruction()).toEqual(undefined);
    expect(getTitleInstruction({})).toEqual(undefined);
    expect(getTitleInstruction({ blocks: [] })).toEqual(undefined);
    expect(getTitleInstruction({ blocks: [{ title: 'Test' }] })).toEqual(undefined);
    expect(getTitleInstruction({ blocks: [{ title: 'Тайтл' }] })).toEqual(undefined);
  });
  it('works with valid data', () => {
    expect(getTitleInstruction({ blocks: [{ title: 'Тайтл', rows: [] }] })).toEqual(undefined);
    expect(
      getTitleInstruction({
        blocks: [
          {
            title: 'Тайтл',
            rows: [
              { title: '', value: '' },
              { title: 'testTitle', value: 'testValue' },
            ],
          },
        ],
      })
    ).toEqual([
      { title: '', value: '' },
      { title: 'testTitle', value: 'testValue' },
    ]);
  });
});
