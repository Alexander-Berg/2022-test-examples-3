import { ModelType, RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { SKUParameterMode } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { getNormalizedModel } from '@yandex-market/mbo-parameter-editor/es';
import { mount } from 'enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import equals from 'ramda/es/equals';

import { RU_ISO_CODE } from 'src/shared/constants';
import { DEFAULT_CATEGORY_ID } from 'src/shared/test-data/test-constants';
import { relation, testModelProtoFactory } from 'src/shared/test-data/test-models';
import { testParameterProto } from 'src/shared/test-data/test-parameters';
import { testCategoryUi, testModelUi } from 'src/shared/test-data/test-ui';
import { SkuList } from 'src/tasks/mapping-moderation/components/SkuList/SkuList';
import { getSafeWord } from 'src/tasks/mapping-moderation/helpers/proto-helpers';
import { categoriesActions } from 'src/tasks/mapping-moderation/store/categories/categories';
import { setupTestStore } from 'src/tasks/mapping-moderation/store/mocks/store';
import modelsActions from 'src/tasks/mapping-moderation/store/models/modelsActions';

jest.useFakeTimers();

describe('SkuList', () => {
  const parentModelId = 100_000;
  const skuParam = testParameterProto({ sku_mode: SKUParameterMode.SKU_DEFINING });
  const category = testCategoryUi({ id: DEFAULT_CATEGORY_ID, parameters: [skuParam] });
  const skuFactory = testModelProtoFactory({
    modelType: ModelType.SKU,
    relations: [relation(RelationType.SKU_PARENT_MODEL, parentModelId)],
    categoryData: category,
  });
  const sku1 = skuFactory();
  const sku1Ui = getNormalizedModel(sku1);
  const sku2 = skuFactory();
  const sku3 = skuFactory();
  const simpleModel = testModelUi();
  const parentModel = testModelUi({
    id: parentModelId,
    relations: [
      relation(RelationType.SKU_MODEL, sku1.id!),
      relation(RelationType.SKU_MODEL, sku2.id!),
      relation(RelationType.SKU_MODEL, sku3.id!),
    ],
  });

  it('should render fine for model not in store', () => {
    const { store } = setupTestStore();
    mount(
      <Provider store={store}>
        <SkuList skuId={sku1.id!} />
      </Provider>
    );
  });

  it('should render fine for model non-sku model (i.e. no parent model)', () => {
    const { store } = setupTestStore();

    store.dispatch(modelsActions.load.done({ result: [simpleModel], params: null as any }));
    const skuList = mount(
      <Provider store={store}>
        <SkuList skuId={simpleModel.id} />
      </Provider>
    );
    expect(skuList).toContainMatchingElement('.SkuList-NotSku');
  });

  it('should load required data', () => {
    const { store, aliasMaker } = setupTestStore();

    store.dispatch(modelsActions.load.done({ result: [sku1Ui], params: null as any }));
    store.dispatch(modelsActions.loadExported.done({ result: [sku1Ui], params: null as any }));
    store.dispatch(categoriesActions.set(category));
    const skuList = mount(
      <Provider store={store}>
        <SkuList skuId={sku1.id!} />
      </Provider>
    );

    expect(skuList).not.toContainMatchingElement('.SkuList-NotSku');
    expect(skuList).toContainMatchingElement('Loading[text^="Загрузка родительской модели"]');

    aliasMaker.findModels.next(r => equals(r.model_ids, [parentModelId])).resolve({ model: [parentModel] });
    jest.runAllTimers();
    skuList.update();

    expect(skuList).not.toContainMatchingElement('Loading[text^="Загрузка родительской модели"]');
    expect(skuList).toContainMatchingElement('Loading[text^="Загрузка SKU"]');

    aliasMaker.findModels.next(r => equals(r.model_ids, [sku2.id!, sku3.id!])).resolve({ model: [sku2, sku3] });
    jest.runAllTimers();
    skuList.update();

    expect(skuList).toContainMatchingElement('Loading[text^="Загрузка SKU"]'); // Requires exported model

    aliasMaker.getModelsExportedCached
      .next(r => equals(r.model_id, [sku2.id!, sku3.id!]))
      .resolve({
        model: [
          {
            ...sku2,
            titles: [{ isoCode: RU_ISO_CODE, value: 'SKU2 exported' }],
          },
          sku3,
        ],
      });
    jest.runAllTimers();
    skuList.update();

    expect(skuList).not.toContainMatchingElement('Loading[text^="Загрузка SKU"]');
    expect(skuList).toContainMatchingElements(3, '.SkuListItem');
    expect(skuList.find('.SkuListItem_id_2 .ModelParameterViewList-Title').text()).toContain('SKU2 exported');

    expect(skuList).toContainMatchingElement(`.SkuListItem_id_${sku1.id}.SkuListItem_active`);
    expect(skuList).toContainMatchingElement(`.SkuListItem_id_${sku2.id}:not(.SkuListItem_active)`);
    expect(skuList).toContainMatchingElement(`.SkuListItem_id_${sku3.id}:not(.SkuListItem_active)`);

    const skuItem = skuList.find(`.SkuListItem_id_${sku1.id}`);
    expect(skuItem.text()).toContain(getSafeWord(skuParam.name!));
  });

  it('should show failures', () => {
    // TODO
  });
});
