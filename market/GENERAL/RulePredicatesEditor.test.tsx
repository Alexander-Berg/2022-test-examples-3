import React from 'react';
import { render } from '@testing-library/react';

import { Subject } from 'src/java/definitions';
import { RulePredicateConditionType } from 'src/components/RulePredicateEditor';
import { RulePredicatesEditor } from './RulePredicatesEditor';

describe('<RulePredicatesEditor />', () => {
  it('renders without errors', () => {
    render(
      <RulePredicatesEditor
        parameters={[]}
        conditionType={RulePredicateConditionType.If}
        predicates={[]}
        subject={Subject.PARAMETER}
        onAdd={jest.fn()}
        onChange={jest.fn()}
        onRemove={jest.fn()}
      />
    );
  });
});
