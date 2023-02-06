import React from 'react';
import { render } from '@testing-library/react';

import { ModelRuleGWTDto } from 'src/java/definitions';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { RuleLinks } from './RuleLinks';

const RULES = [
  {
    categoryId: 1,
    modelRule: {
      id: 2,
      name: 'test2',
      group: 'group2',
      priority: 20,
      active: true,
    },
  },
] as ModelRuleGWTDto[];

describe('<RuleLinks />', () => {
  it('render without errors', () => {
    const Provider = setupTestProvider();

    render(
      <Provider>
        <RuleLinks rules={RULES} />
      </Provider>
    );
  });
});
