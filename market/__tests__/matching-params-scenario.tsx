import { getNormalizedCategoryData, getNormalizedModel } from '@yandex-market/mbo-parameter-editor/es';
import { createApiMock } from '@yandex-market/mbo-test-utils';
import { mount } from 'enzyme';
import * as React from 'react';

import { AliasMaker } from 'src/shared/services';
import { RUSSIAN_LANG_ID } from 'src/shared/constants';
import { testCategoryProto } from 'src/shared/test-data/test-categories';
import { testModelProto } from 'src/shared/test-data/test-models';
import { testGskuInput, testOffer } from 'src/shared/test-data/test-offers';
import { testParameterProto } from 'src/shared/test-data/test-parameters';
import { GeneratedSkuInfoInner } from 'src/tasks/mapping-moderation/components/GeneratedSkuInfo/GeneratedSkuInfo';
import { Highlight } from 'src/tasks/mapping-moderation/components/Image/Highlight/Highlight';
import { ModelInfo } from 'src/tasks/mapping-moderation/components/ModelInfo/ModelInfo';
import { ModerationApp } from 'src/tasks/mapping-moderation/components/ModerationApp/ModerationApp';
import { OfferInfoDumb } from 'src/tasks/mapping-moderation/components/OfferInfo/OfferInfo';
import { MappingModerationInput, ModerationTaskType } from 'src/tasks/mapping-moderation/helpers/input-output';
import { getFirstModelParamSimpleValue } from 'src/tasks/mapping-moderation/helpers/model-helpers';
import initializeModerationDataRequests from 'src/tasks/mapping-moderation/helpers/test/initializeModerationDataRequests';
import { XslNames } from 'src/tasks/mapping-moderation/helpers/xsl-names';

/**
 * Test is on global level as it's important to test all mapToState, caches, etc.
 */
describe('ModerationApp', () => {
  const setup = () => {
    const aliasMaker = createApiMock<AliasMaker>();
    const barCode = testParameterProto({ xsl_name: XslNames.BAR_CODE });
    const vendorCode = testParameterProto({ xsl_name: XslNames.VENDOR_CODE });
    const category = testCategoryProto({ parameters: [barCode, vendorCode] });
    const categoryData = getNormalizedCategoryData(category);
    const sku = testModelProto({ categoryData });

    return { aliasMaker, category, categoryData, barCode, sku };
  };

  it('should highlight matching params in offer to model scenario', async () => {
    const { aliasMaker, sku, category, categoryData } = setup();
    const skuUi = getNormalizedModel(sku);

    const skuBarcode = getFirstModelParamSimpleValue(XslNames.BAR_CODE, skuUi, categoryData) as string;
    const offer = testOffer({
      supplierSkuId: sku.id,
      barcode: skuBarcode,
      vendor_code: 'Some other value',
    });

    const input: MappingModerationInput = {
      // any is kind of hack here, to use common mock instead of String version
      offers: [offer as any],
      task_type: ModerationTaskType.MAPPING_MODERATION,
    };
    const app = mount(<ModerationApp aliasMaker={aliasMaker} onSubmit={() => undefined} input={input} />);

    await initializeModerationDataRequests(aliasMaker, category, [sku], app, [offer]);

    app.update();

    const modelInfo = app.find(ModelInfo);
    // Displayed exactly twice: in short info and in model form (it contains all params)
    expect(modelInfo.find(Highlight).map(h => h.text())).toEqual([skuBarcode, skuBarcode]);

    const offerInfo = app.find(OfferInfoDumb);
    expect(offerInfo.find(Highlight).map(h => h.text())).toEqual([skuBarcode]);
  });

  it('should highlight matching params in gsku scenario', async () => {
    const { aliasMaker, sku, category, categoryData } = setup();
    const skuUi = getNormalizedModel(sku);
    const gsku = testModelProto({ categoryData });
    // Drop vendorCode to check both cases
    gsku.parameter_values = (gsku.parameter_values || []).filter(pv => pv.xsl_name !== XslNames.VENDOR_CODE);

    const skuBarcode = getFirstModelParamSimpleValue(XslNames.BAR_CODE, skuUi, categoryData) as string;
    const gskuInput = testGskuInput({ supplierSkuId: sku.id!, generated_sku_id: `${gsku.id}` });

    const input: MappingModerationInput = { offers: [gskuInput] };
    const app = mount(<ModerationApp aliasMaker={aliasMaker} onSubmit={() => undefined} input={input} />);

    await initializeModerationDataRequests(aliasMaker, category, [sku, gsku], app);

    app.update();

    const modelInfo = app.find(ModelInfo);
    // Displayed exactly twice: in short info and in model form (it contains all params)
    expect(modelInfo.find(Highlight).map(h => h.text())).toEqual([skuBarcode, skuBarcode]);

    const generatedSkuInfo = app.find(GeneratedSkuInfoInner);
    expect(generatedSkuInfo.find(Highlight).map(h => h.text())).toEqual([skuBarcode, skuBarcode]);
  });

  it('should highlight matching params in gsku for hypothesis', async () => {
    const { aliasMaker, sku, category, categoryData, barCode } = setup();
    const skuUi = getNormalizedModel(sku);
    const skuBarcode = getFirstModelParamSimpleValue(XslNames.BAR_CODE, skuUi, categoryData) as string;

    const gsku = testModelProto({ categoryData });
    // Drop vendorCode to check both cases
    gsku.parameter_values = [];
    gsku.parameter_value_hypothesis = [
      {
        param_id: barCode.id,
        xsl_name: barCode.xsl_name,
        str_value: [{ name: skuBarcode, lang_id: RUSSIAN_LANG_ID }],
      },
    ];

    const gskuInput = testGskuInput({ supplierSkuId: sku.id!, generated_sku_id: `${gsku.id}` });

    const input: MappingModerationInput = { offers: [gskuInput] };
    const app = mount(<ModerationApp aliasMaker={aliasMaker} onSubmit={() => undefined} input={input} />);

    await initializeModerationDataRequests(aliasMaker, category, [sku, gsku], app);

    app.update();

    const modelInfo = app.find(ModelInfo);
    // Displayed exactly twice: in short info and in model form (it contains all params)
    expect(modelInfo.find(Highlight).map(h => h.text())).toEqual([skuBarcode, skuBarcode]);

    const generatedSkuInfo = app.find(GeneratedSkuInfoInner);
    expect(generatedSkuInfo.find(Highlight).map(h => h.text())).toEqual([skuBarcode, skuBarcode]);
  });
});
