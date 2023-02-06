import { IGetBaobabAttrsArgs as ICommonBaobab } from 'mg/lib/getBaobabAttrs';

import { EUrlParamName as ESportUrlParamName } from 'sport/const/EUrlParamName';
import { getBaobabAttrs, IGetBaobabAttrsArgs } from '../getBaobabAttrs';

jest.mock('mg/lib/getBaobabAttrs', () => {
  const original = jest.requireActual('mg/lib/getBaobabAttrs');

  return {
    ...original,
    getBaobabAttrs(args: ICommonBaobab) {
      return { ...args };
    },
  };
});

describe('getBaobabAttrs', () => {
  afterAll(() => {
    jest.unmock('mg/lib/getBaobabAttrs');
  });

  it('без isAjax не происходит модификация аттрибутов', () => {
    const result = getBaobabAttrs({
      isAjax: false,
      isAab: false,
      request: {
        params: {
          [ESportUrlParamName.PARENT_PAGE]: ['this param wont be returned'],
        },
      },
    } as unknown as IGetBaobabAttrsArgs);

    expect(result).toMatchObject({ isAab: false });
  });

  it('с isAjax добавляется page', () => {
    const result = getBaobabAttrs({
      isAjax: true,
      isAab: false,
      request: {
        params: {
          [ESportUrlParamName.PARENT_PAGE]: ['testPage'],
        },
      },
    } as unknown as IGetBaobabAttrsArgs);

    expect(result).toMatchObject({ isAab: false, page: 'testPage' });
  });
});
