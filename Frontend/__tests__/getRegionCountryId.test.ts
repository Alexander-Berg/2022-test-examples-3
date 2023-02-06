import { getRegionCountryId } from 'sport/lib/getRegionCountryId';
import { IRegion } from 'neo/types/apphost';

describe('getLinkPage', () => {
  it('Возвращает undefined, если регион не передан', () => {
    const result = getRegionCountryId(undefined);
    expect(result).toBe(undefined);
  });

  it('Возвращает tuned.country_id, если это поле есть и содержит id региона', () => {
    const result = getRegionCountryId(getMockRegion({
      tuned: {
        country_id: 111,
        path: [1],
        id: 1,
      },
    }));

    expect(result).toBe(111);
  });

  it('Возвращает real.country_id, если это поле есть и содержит id региона', () => {
    const result = getRegionCountryId(getMockRegion({
      real: {
        country_id: 222,
        path: [1],
        id: 1,
      },
    }));

    expect(result).toBe(222);
  });

  it('Возвращает default.country_id, если это поле есть и содержит id региона', () => {
    const result = getRegionCountryId(getMockRegion());

    expect(result).toBe(333);
  });
});

function getMockRegion(data?: Partial<IRegion>): IRegion {
  return {
    type: 'region',
    is_manual: false,
    default: {
      country_id: 333,
      path: [3],
      id: 3,
    },
    lr: {
      country_id: 44,
      path: [4],
      id: 4,
    },
    ...data,
  };
}
