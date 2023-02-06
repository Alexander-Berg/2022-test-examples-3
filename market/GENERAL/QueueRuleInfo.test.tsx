import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { QueueRuleInfo } from './QueueRuleInfo';

describe('<QueueRuleInfo />', () => {
  it('render without errors', () => {
    const Provider = setupTestProvider();

    render(
      <Provider>
        <QueueRuleInfo categoryId={0} />
      </Provider>
    );
  });
});
