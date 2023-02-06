import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import 'news/tests/mocks/hooks/contexts/useDataSourceCtx';

import { StorySettings } from '../Story__Settings.desktop';

Enzyme.configure({ adapter: new Adapter() });

describe('StorySettings', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('Should show share and stat menu item', () => {
    component = mount(
      <StorySettings
        share={{
          url: '/',
          description: 'Заголовок новости',
          title: 'Поделиться',
        }}
        stat={{
          lastHourDocuments: 1,
          relatedStories: 1,
          totalDocuments: 1,
        }}
      />,
    );

    expect(component.html()).toMatchSnapshot();
  });
});
