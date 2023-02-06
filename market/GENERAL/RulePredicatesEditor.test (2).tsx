import React from 'react';
import { render } from '@testing-library/react';

import { PredicateType } from 'src/java/definitions';
import { RulePredicatesEditor } from './RulePredicatesEditor';

describe('<RulePredicatesEditor />', () => {
  it('renders without errors', () => {
    render(
      <RulePredicatesEditor
        ruleId={0}
        parameters={[]}
        conditionType={PredicateType.IF}
        predicates={[]}
        onAdd={jest.fn()}
        onChange={jest.fn()}
        onRemove={jest.fn()}
      />
    );
  });
});
