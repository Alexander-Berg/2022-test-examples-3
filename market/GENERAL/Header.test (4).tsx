import { mount } from 'enzyme';
import React from 'react';

import { Header } from './Header';

describe('Header', () => {
  it('should renders', () => {
    const wrapper = mount(
      <Header
        logoProps={{ title: 'test' }}
        backgroundColor="blue"
        headerColor="white"
        menuProps={{
          items: [
            {
              id: 'tab2',
              label: 'tab2-content',
              href: '/',
            },
            {
              id: 'tab3',
              label: 'tab3-content',
              subMenu: [
                { id: 'tab31', label: 'tab1-content', href: '/' },
                { id: 'tab32', label: 'tab1-content', href: '/' },
                { id: 'tab33', label: 'tab1-content', href: '/' },
              ],
            },
          ],
        }}
      />
    );

    expect(wrapper).toBeDefined();
  });
});
