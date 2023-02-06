import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { getNormalizedCategoryData } from '@yandex-market/mbo-parameter-editor/es';
import { getNormalizedModelForm } from '@yandex-market/mbo-parameter-editor/es/entities/modelForm/normalize';
import { mount } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';

import { defaultValueRenderer } from '../ModelParameterView/ModelParameterView';
import { testCategoryProto, testFormProto } from 'src/shared/test-data/test-categories';
import { relation, testPicture } from 'src/shared/test-data/test-models';
import { testCategoryUi, testModelUi } from 'src/shared/test-data/test-ui';
import { ModelInfoInner } from './ModelInfo';
import { setupTestStore } from 'src/tasks/mapping-moderation/store/mocks/store';

jest.useFakeTimers();

describe('ModelInfo', () => {
  describe('renders', () => {
    const model = testModelUi();
    const categoryData = testCategoryUi({ id: model.categoryId });
    const modelForm = getNormalizedModelForm(testFormProto({ hid: model.categoryId, parameter: [] }));

    it('without modelId', () => {
      const modelInfo = mount(<ModelInfoInner renderer={defaultValueRenderer} skuDefiningParamIds={[]} />);

      expect(modelInfo).toContainMatchingElement('.ModelInfo-NoSku');
    });

    it('without model', () => {
      const modelInfo = mount(
        <ModelInfoInner modelId={model.id} renderer={defaultValueRenderer} skuDefiningParamIds={[]} />
      );

      expect(modelInfo).toContainMatchingElement('.ModelInfo-ModelLoading');
    });

    it('without category data', () => {
      const modelInfo = mount(
        <ModelInfoInner modelId={model.id} model={model} renderer={defaultValueRenderer} skuDefiningParamIds={[]} />
      );

      expect(modelInfo).toContainMatchingElement('.ModelInfo-CategoryLoading');
    });

    it('without model form', () => {
      const modelInfo = mount(
        <ModelInfoInner
          modelId={model.id}
          model={model}
          category={categoryData}
          renderer={defaultValueRenderer}
          skuDefiningParamIds={[]}
        />
      );

      expect(modelInfo).toContainMatchingElement('.ModelInfo-ModelFormLoading');
    });

    it('with all required data', () => {
      const modelInfo = mount(
        <ModelInfoInner
          modelId={model.id}
          model={model}
          category={categoryData}
          renderer={defaultValueRenderer}
          modelForm={modelForm}
          skuDefiningParamIds={[]}
        />
      );

      expect(modelInfo).toContainMatchingElement('.ModelParameterViewList-Title');
    });
  });

  it('Should switch SKU tab correctly', () => {
    const { store } = setupTestStore();

    const parentModelId = 100_000;
    const sku1 = testModelUi({ relations: [relation(RelationType.SKU_PARENT_MODEL, parentModelId)] });
    const categoryData = testCategoryUi({ id: sku1.categoryId });
    const modelForm = getNormalizedModelForm(testFormProto({ hid: sku1.categoryId, parameter: [] }));

    const modelInfo = mount(
      <Provider store={store}>
        <ModelInfoInner
          modelId={sku1.id}
          model={sku1}
          category={categoryData}
          modelForm={modelForm}
          renderer={defaultValueRenderer}
          skuDefiningParamIds={[]}
        />
      </Provider>
    );

    expect(modelInfo.find('.ModelInfo-ModelLoading')).not.toExist();
    expect(modelInfo.find('.ModelInfo-CategoryLoading')).not.toExist();
    expect(modelInfo.find('.SkuList')).not.toExist();

    const skuTab = modelInfo.find('button.Tab[data-id="SKU-Tab"]');

    skuTab.simulate('click');

    expect(modelInfo.find('.SkuList')).toExist();
  });

  it('should renderer pictures', () => {
    const model = testModelUi();
    model.normalizedPictures = [];
    model.normalizedPictures.push(
      testPicture({ url: 'pic1.jpeg' }),
      testPicture({ url: 'pic1.jpeg' }), // duplicate should be removed
      testPicture({ url: 'pic2.jpeg' })
    );
    const protoCategory = testCategoryProto();
    const category = getNormalizedCategoryData(protoCategory);
    const modelForm = getNormalizedModelForm(testFormProto(protoCategory));

    const modelInfo = mount(
      <ModelInfoInner
        model={model}
        category={category}
        modelId={model.id}
        modelForm={modelForm}
        skuDefiningParamIds={[]}
        renderer={defaultValueRenderer}
      />
    );
    expect(modelInfo).toContainMatchingElement('.Images-SelectedImage');
    expect(modelInfo.find('.Images-GalleryImage Image').map(i => i.prop('src'))).toContainValues([
      'pic1.jpeg',
      'pic2.jpeg',
    ]);
  });
});
