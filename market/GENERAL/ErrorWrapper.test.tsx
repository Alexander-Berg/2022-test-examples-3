import { mount, ReactWrapper } from 'enzyme';
import React from 'react';

import { ErrorWrapper } from './ErrorWrapper';

let wrapper: ReactWrapper;

describe('Error wrapper', () => {
  it('main flow', () => {
    wrapper = mount(
      <ErrorWrapper showError errorText="текст">
        <span>TestContent</span>
      </ErrorWrapper>
    );
    expect(wrapper.find({ children: 'TestContent' }).length).toEqual(1);
  });
});
