import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor';

import { moveSkus } from './moveSkus';

describe('moveSkus', () => {
  it('works', () => {
    const pickerParameterId = 4123;
    const optionId = 5123;

    const sourceModel: ProtoModel = {
      category_id: 1123,
      id: 3123,
      parameter_value_links: [
        {
          image_picker: { url: 'test url' },
          param_id: pickerParameterId,
          option_id: optionId,
        },
      ],
    };

    const sku: ProtoModel = {
      category_id: 1123,
      vendor_id: 2123,
      id: 3234,
      relations: [
        {
          id: 3123,
          category_id: 1123,
          type: RelationType.SKU_PARENT_MODEL,
        },
      ],
      parameter_values: [{ param_id: pickerParameterId, option_id: optionId }],
    };

    const targetModel: ProtoModel = {
      id: 3345,
      vendor_id: 2234,
      category_id: 1123,
    };

    expect(
      moveSkus({
        sourceModel,
        skus: [sku],
        targetModel,
        pickerParameterId,
      })
    ).toEqual([
      {
        id: 3345,
        vendor_id: 2234,
        category_id: 1123,
        parameter_value_links: [{ image_picker: { url: 'test url' }, param_id: 4123, option_id: 5123 }],
      },
      {
        category_id: 1123,
        vendor_id: 2234,
        id: 3234,
        relations: [{ id: 3345, category_id: 1123, type: 'SKU_PARENT_MODEL' }],
        parameter_values: [
          { param_id: 4123, option_id: 5123 },
          { param_id: 7351771, type_id: 4, xsl_name: 'name', value_source: 'OPERATOR_FILLED', value_type: 'STRING' },
          {
            param_id: 7893318,
            type_id: 1,
            option_id: 2234,
            xsl_name: 'vendor',
            value_source: 'AUTO',
            value_type: 'ENUM',
          },
        ],
      },
    ]);
  });
});
