import React from 'react';
import { render } from '@testing-library/react';

import { ModelRuleDto } from 'src/java/definitions';
import { RuleCloseButton } from './RuleCloseButton';

const RULE = {
  id: 1,
  name: 'test1',
  group: 'group1',
  priority: 10,
  active: true,
} as ModelRuleDto;

describe('<RuleCloseButton />', () => {
  it('render without errors', () => {
    render(<RuleCloseButton rule={RULE} onDelete={jest.fn()} />);
  });
});
