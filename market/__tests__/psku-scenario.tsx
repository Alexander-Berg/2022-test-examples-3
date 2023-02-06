import React from 'react';
import { mount } from 'enzyme';
import { ModelType, Model } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { Category } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { getNormalizedCategoryData } from '@yandex-market/mbo-parameter-editor';
import { createApiMock, MockedApi } from '@yandex-market/mbo-test-utils';

import { AliasMaker } from 'src/shared/services';
import { Button } from 'src/shared/components';
import { testCategoryProto, testFormProto } from 'src/shared/test-data/test-categories';
import { testModelProto } from 'src/shared/test-data/test-models';
import { equalsIgnoreOrder } from 'src/shared/utils/ramda-helpers';
import { wait } from 'src/shared/utils/testing/utils';
import { GeneratedSkuInfoInner } from 'src/tasks/mapping-moderation/components/GeneratedSkuInfo/GeneratedSkuInfo';
import simpleTask from 'src/tasks/mapping-moderation/components/ModerationApp/__tests__/pksu-scenario-task';
import { ModerationApp } from 'src/tasks/mapping-moderation/components/ModerationApp/ModerationApp';
import { ModerationSceneLoader } from 'src/tasks/mapping-moderation/components/ModerationSceneLoader/ModerationSceneLoader';
import {
  MappingModerationInput,
  MappingModerationOutput,
  MappingModerationStatus,
} from 'src/tasks/mapping-moderation/helpers/input-output';

describe('Partner SKU behavior', () => {
  const CATEGORY_ID = 1;

  const setup = (input: MappingModerationInput) => {
    const aliasMaker = createApiMock<AliasMaker>();
    const submitHolder = { submit: null as unknown as () => Promise<MappingModerationOutput | undefined> };

    const onSubmit = (handler: () => Promise<MappingModerationOutput | undefined>) => {
      submitHolder.submit = handler;
    };
    const app = mount(<ModerationApp aliasMaker={aliasMaker} onSubmit={onSubmit} input={input} />);

    return { app, aliasMaker, submitHolder };
  };

  const resolveRequests = ({
    aliasMaker,
    models,
    category,
  }: {
    aliasMaker: MockedApi<AliasMaker>;
    models: Model[];
    category: Category;
  }) => {
    const modelIds = models.map(item => item.id);

    aliasMaker.findModels.next(r => equalsIgnoreOrder(r.model_ids!, modelIds)).resolve({ model: models });

    models.forEach(item => {
      aliasMaker.getModelsExportedCached
        .next(r => equalsIgnoreOrder(r.model_id!, [item.id]))
        .resolve({ model: [item] });
    });

    aliasMaker.getParameters
      .next(r => r.category_id === category.hid)
      .resolve({ response: { category_parameters: category } });

    aliasMaker.getModelForms
      .next(r => equalsIgnoreOrder(r.category_ids || [], [category.hid]))
      .resolve({ response: { model_forms: [testFormProto(category)] } });
  };

  it('Should check task as PSKU moderation', async () => {
    const { app, aliasMaker } = setup(simpleTask as unknown as MappingModerationInput);
    const category = testCategoryProto({
      id: CATEGORY_ID,
      parameters: [],
    });
    const categoryData = getNormalizedCategoryData(category);
    const psku = testModelProto({ id: 1001, categoryData, modelType: ModelType.PARTNER_SKU });
    const sku = testModelProto({ id: 1002, categoryData });

    await wait(1);

    resolveRequests({ aliasMaker, models: [psku, sku], category });
    app.update();

    // test that the button is available
    expect(
      app
        .find(GeneratedSkuInfoInner)
        .find(Button)
        .filterWhere(item => item.text() === 'Нужна доп. информация >')
    ).toHaveLength(1);
  });

  it('Should check deleted offer is not available', async () => {
    const { app, aliasMaker, submitHolder } = setup(simpleTask as any as MappingModerationInput);
    const category = testCategoryProto({
      id: CATEGORY_ID,
      parameters: [],
    });
    const categoryData = getNormalizedCategoryData(category);
    const psku = testModelProto({ id: 1001, categoryData, modelType: ModelType.PARTNER_SKU, isDeleted: true });
    const sku = testModelProto({ id: 1002, categoryData });

    await wait(1);

    resolveRequests({ aliasMaker, models: [psku, sku], category });
    app.update();

    // test message in UI
    expect(app.find(ModerationSceneLoader).text()).toContain(
      'У оферов обновился статус и они более недоступны. Нажмите кнопку «Отправить» для завершения задания'
    );

    // test submit result
    const taskResult = await submitHolder.submit();
    expect(
      taskResult!.results.every(
        item => 'deleted' in item && item.deleted && item && item.status === MappingModerationStatus.UNDEFINED
      )
    ).toBeTrue();
  });
});
