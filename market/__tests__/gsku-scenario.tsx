import { SKUParameterMode } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { Status } from '@yandex-market/market-proto-dts/Market/Mboc/SearchMappingsResponse';
import { getNormalizedCategoryData } from '@yandex-market/mbo-parameter-editor/es';
import { createApiMock } from '@yandex-market/mbo-test-utils';
import { mount } from 'enzyme';
import React from 'react';
import { ToastContainer } from 'react-toastify';

import { Button } from 'src/shared/components';
import { AliasMaker } from 'src/shared/services';
import { testCategoryProto, testFormProto } from 'src/shared/test-data/test-categories';
import { testModelProto } from 'src/shared/test-data/test-models';
import { testParameterProto } from 'src/shared/test-data/test-parameters';
import { equalsIgnoreOrder } from 'src/shared/utils/ramda-helpers';
import { wait } from 'src/shared/utils/testing/utils';
import { GeneratedSkuInfoInner } from 'src/tasks/mapping-moderation/components/GeneratedSkuInfo/GeneratedSkuInfo';
import { ModerationApp } from 'src/tasks/mapping-moderation/components/ModerationApp/ModerationApp';
import { OfferResult } from 'src/tasks/mapping-moderation/components/OfferResult/OfferResult';
import { OfferListOfferDumb } from 'src/tasks/mapping-moderation/components/OffersList/Offer/OfferListOffer';
import {
  MappingModerationInput,
  MappingModerationOutput,
  MappingModerationStatus,
  ModerationTaskType,
} from 'src/tasks/mapping-moderation/helpers/input-output';

describe('Generated SKU behavior', () => {
  const CATEGORY_ID = 1;

  const setup = () => {
    const aliasMaker = createApiMock<AliasMaker>();
    const submitHolder = { submit: null as unknown as () => Promise<MappingModerationOutput | undefined> };
    const input: MappingModerationInput = {
      offers: [
        {
          id: '1',
          generated_sku_id: '1001',
          supplier_mapping_info: {
            sku_id: '1002',
          },
          category_id: '1',
          category_name: 'test',
        },
      ],
      task_type: ModerationTaskType.BETTER_CONTENT_MODERATION,
    };
    const onSubmit = (handler: () => Promise<MappingModerationOutput | undefined>) => {
      submitHolder.submit = handler;
    };
    const app = mount(<ModerationApp aliasMaker={aliasMaker} onSubmit={onSubmit} input={input} />);

    return { app, aliasMaker, submitHolder };
  };

  it('Should display loader', async () => {
    const { app, aliasMaker, submitHolder } = setup();

    const category = testCategoryProto({
      id: CATEGORY_ID,
      parameters: [testParameterProto({ sku_mode: SKUParameterMode.SKU_DEFINING })],
    });
    const categoryData = getNormalizedCategoryData(category);
    const gsku = testModelProto({ id: 1001, categoryData });
    const sku = testModelProto({ id: 1002, categoryData });

    aliasMaker.getContentCommentTypes.next().resolve({ response: { content_comment_type: [] } });

    await wait(1);
    app.update();

    expect(app.find(OfferListOfferDumb).text()).toContain(`Загрузка GSKU #${gsku.id}`);

    aliasMaker.findModels
      .next(r => equalsIgnoreOrder(r.model_ids || [], [gsku.id, sku.id]))
      .resolve({ model: [gsku, sku] });

    app.update();
    expect(app.find(OfferListOfferDumb).text()).toContain(`Загрузка GSKU #${gsku.id}`);

    aliasMaker.getModelsExportedCached
      .next(r => equalsIgnoreOrder(r.model_id!, [gsku.id, sku.id]))
      .resolve({ model: [gsku, sku] });

    app.update();
    expect(app.find(OfferListOfferDumb).text()).toContain(gsku.titles![0].value);
    expect(app.find(GeneratedSkuInfoInner).text()).toContain('Загрузка категории');

    aliasMaker.getParameters
      .next(r => r.category_id === CATEGORY_ID)
      .resolve({ response: { category_parameters: category } });

    app.update();
    expect(app.find(GeneratedSkuInfoInner).text()).toContain('Загрузка операторской карточки');

    aliasMaker.getModelForms
      .next(r => equalsIgnoreOrder(r.category_ids || [], [CATEGORY_ID]))
      .resolve({ response: { model_forms: [testFormProto(category)] } });

    aliasMaker.searchBaseOfferMappingsByMarketSkuId.next().resolve({ reqId: '', status: Status.OK, offers: [] });
    aliasMaker.searchBaseOfferMappingsByMarketSkuId.next().resolve({ reqId: '', status: Status.OK, offers: [] });

    expect(aliasMaker.activeRequests().map(r => r.name)).toBeEmpty();

    app.update();

    // Check some info is displayed
    const generatedSkuInfoText = app.find(GeneratedSkuInfoInner).text();
    expect(generatedSkuInfoText).toContain(gsku.titles![0].value);
    expect(generatedSkuInfoText).toContain(gsku.parameter_values![0].str_value![0].value);

    // Task isn't PSKU moderation and buttons of need info not availables
    expect(
      app
        .find(GeneratedSkuInfoInner)
        .find(Button)
        .filterWhere(item => item.text() === 'Нужна доп. информация')
    ).toHaveLength(0);

    const error = await submitHolder.submit();

    await wait();

    app.update();

    expect(error).toBeUndefined();
    expect(app.find(ToastContainer).html()).toContain('#1 не заполнен ответ');

    // Set value
    const results = app.find(OfferResult);
    results.find('.testid-accepted button').simulate('click');

    const result: MappingModerationOutput | undefined = await submitHolder.submit();
    expect(result as MappingModerationOutput).toEqual({
      results: [
        {
          generated_sku_id: gsku.id,
          msku: sku.id,
          req_id: 1,
          status: MappingModerationStatus.ACCEPTED,
        },
      ],
    });
  });
});
