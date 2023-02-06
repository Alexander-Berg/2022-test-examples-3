import React from 'react';
import { render } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { RuleEditorLink } from './RuleEditorLink';

describe('<RuleEditorLink />', () => {
  it('render without errors', () => {
    const Provider = setupTestProvider();

    render(
      <Provider>
        <RuleEditorLink ruleId={1} />
      </Provider>
    );
  });
});
