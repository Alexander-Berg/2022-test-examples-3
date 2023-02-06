import { SuggestedModel } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { mount } from 'enzyme';
import React from 'react';

import { Spin } from 'src/shared/components';
import { TextInput } from 'src/shared/components/TextInput/TextInput';
import { ModelCard } from '../ModelCard/ModelCard';
import { ModelsBlock } from './ModelsBlock';
import { useLoadModels } from './useLoadModels';

jest.mock('./useLoadModels');

const useLoadModelsMock = useLoadModels as jest.Mock<{ models: SuggestedModel[]; isLoading: boolean }>;

describe('ModelsBlock', () => {
  it('renders loading', () => {
    useLoadModelsMock.mockReturnValue({ models: [], isLoading: true });
    const wrapper = mount(<ModelsBlock categoryId={123} onModelSelect={jest.fn()} selectedVendorId={312} />);

    expect(wrapper.find(Spin)).toHaveLength(1);
  });
  it('renders without vendor', () => {
    useLoadModelsMock.mockReturnValue({ models: [], isLoading: false });
    const wrapper = mount(<ModelsBlock categoryId={123} onModelSelect={jest.fn()} />);

    expect(wrapper.html()).toContain('Выберите вендора');
  });
  it('renders without models', () => {
    useLoadModelsMock.mockReturnValue({ models: [], isLoading: false });
    const wrapper = mount(<ModelsBlock categoryId={123} onModelSelect={jest.fn()} selectedVendorId={321} />);

    expect(wrapper.html()).toInclude('Не найдено моделей удовлетворяющих условиям запроса');
  });
  it('renders', () => {
    const models = [{ name: 'testModel', id: 123456, picture: 'testUrl' }];
    useLoadModelsMock.mockReturnValue({ models, isLoading: false });
    const wrapper = mount(<ModelsBlock categoryId={123} onModelSelect={jest.fn()} selectedVendorId={321} />);

    expect(wrapper.find(TextInput)).toHaveLength(1);
    expect(wrapper.find(ModelCard)).toHaveLength(1);
  });
  it('selects', () => {
    const models = [
      { name: 'testModel321', id: 123456, picture: 'testUrl' },
      { name: 'testModel435', id: 123457, picture: 'testUrl' },
      { name: 'testModel678', id: 123678, picture: 'testUrl' },
    ];
    let selectedModel;

    useLoadModelsMock.mockReturnValue({ models, isLoading: false });
    const wrapper = mount(
      <ModelsBlock
        categoryId={123}
        onModelSelect={m => {
          selectedModel = m;
        }}
        selectedVendorId={321}
      />
    );

    wrapper
      .find(ModelCard)
      .findWhere(i => i.prop('model')?.id === models[1].id)
      .simulate('click');

    expect(selectedModel).toEqual(models[1]);
  });
});
