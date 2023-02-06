import { mount } from 'enzyme';
import React from 'react';

import { ModelCard } from 'src/shared/components/ModelSelectModal/ModelCard/ModelCard';

describe('ModelCard', () => {
  it('renders with image', () => {
    const model = { name: 'testModel', id: 123456, picture: 'testUrl' };
    const wrapper = mount(<ModelCard model={model} onModelSelect={jest.fn()} />);

    expect(wrapper.html()).toInclude(model.name);
    expect(wrapper.html()).toInclude(String(model.id));
    expect(wrapper.find('img')).toHaveLength(1);
    expect(wrapper.find('img').prop('src')).toEqual(model.picture);
  });
  it('renders without image', () => {
    const model = { name: 'testModel', id: 123456 };
    const wrapper = mount(<ModelCard model={model} onModelSelect={jest.fn()} />);

    expect(wrapper.html()).toInclude(model.name);
    expect(wrapper.html()).toInclude(String(model.id));
    expect(wrapper.find('img')).toHaveLength(0);
  });
  it('triggers onSelect', () => {
    const model = { name: 'testModel', id: 123456, picture: 'testUrl' };
    let selectedModel;

    const wrapper = mount(
      <ModelCard
        model={model}
        onModelSelect={m => {
          selectedModel = m;
        }}
      />
    );

    wrapper.find('div').first().simulate('click');

    expect(selectedModel).toEqual(model);
  });
});
