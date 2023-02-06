import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { GroupRules } from './GroupRules';

describe('<GroupRules />', () => {
  it('render without errors', () => {
    const Provider = setupTestProvider();

    render(
      <Provider>
        <GroupRules />
      </Provider>
    );
  });
});
