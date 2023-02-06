import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { isParamsMatchFilter } from './isParamsMatchFilter';
import { RU_ISO_CODE } from './constants';

const testParam1 = { parameterId: 1, type: ValueType.STRING, stringValue: [{ value: 'test1', isoCode: RU_ISO_CODE }] };
const testParam2 = { parameterId: 1, type: ValueType.STRING, stringValue: [{ value: 'test2', isoCode: RU_ISO_CODE }] };

describe('isParamsMatchFilter', () => {
  it('works', () => {
    expect(
      isParamsMatchFilter({ [testParam1.parameterId]: [testParam1] }, { [testParam1.parameterId]: [testParam1] })
    ).toEqual(true);
    expect(
      isParamsMatchFilter({ [testParam1.parameterId]: [testParam1] }, { [testParam2.parameterId]: [testParam2] })
    ).toEqual(false);
  });
  it('works with empty filter', () => {
    expect(isParamsMatchFilter({ [testParam1.parameterId]: [testParam1] }, { [testParam1.parameterId]: [] })).toEqual(
      true
    );
  });
  it('works with empty model', () => {
    expect(isParamsMatchFilter({ [testParam1.parameterId]: [] }, { [testParam1.parameterId]: [testParam1] })).toEqual(
      false
    );
  });
});
