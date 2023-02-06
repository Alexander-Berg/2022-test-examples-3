import React from 'react';
import { mount } from 'enzyme';

import { CategorySelect } from '.';

describe('CategorySelect', () => {
  it('renders', () => {
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    const wrapper = mount(<CategorySelect options={[]} onChange={() => {}} />);

    expect(wrapper).toBeDefined();
  });
});
