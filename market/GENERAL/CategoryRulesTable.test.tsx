import React from 'react';
import { render } from '@testing-library/react';

import { GLRuleDto, GLRuleType } from 'src/java/definitions';
import { CategoryRulesTable } from '.';

describe('<CategoryRulesTable />', () => {
  it('renders without errors', () => {
    const rules: GLRuleDto[] = [
      {
        categoryHid: 1,
        id: 1,
        ifs: [],
        inherited: false,
        name: 'test',
        published: true,
        thens: [],
        type: GLRuleType.MANUAL,
        weight: 100,
      },
    ];

    render(<CategoryRulesTable rules={rules} onRemove={jest.fn()} onUpdate={jest.fn()} onEdit={jest.fn()} />);
  });
});
