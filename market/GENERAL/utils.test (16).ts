import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { getOtherSkus } from 'src/components/SkuImageCopyModal/utils';

const EMPTY_SKU = partialWrapper<NormalisedModel>({});

describe('getOtherSkus', () => {
  it.each`
    sku          | models | expected | description
    ${EMPTY_SKU} | ${{}}  | ${[]}    | ${'no parentId'}
  `(`work with: $description`, ({ sku, models, expected }) => {
    expect(getOtherSkus(sku, models)).toEqual(expected);
  });
});
