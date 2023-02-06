import React from 'react';
import { render } from '@testing-library/react';

import { ModelRuleDto } from 'src/java/definitions';
import { ChangeActiveCheckbox } from './ChangeActiveCheckbox';

const RULE = {
  id: 1,
  name: 'test1',
  group: 'group1',
  priority: 10,
  active: true,
} as ModelRuleDto;

describe('<ChangeActionCheckbox />', () => {
  it('render without errors', () => {
    render(<ChangeActiveCheckbox rule={RULE} onChangeRule={jest.fn()} />);
  });
});
