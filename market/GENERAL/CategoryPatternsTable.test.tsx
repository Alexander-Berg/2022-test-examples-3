import React from 'react';
import { render } from '@testing-library/react';

import { GLPatternDto } from 'src/java/definitions';
import { CategoryPatternsTable } from '.';

describe('<CategoryPatternsTable />', () => {
  it('renders without errors', () => {
    const patterns: GLPatternDto[] = [
      {
        categoryHid: 1,
        container: 'категория',
        global: false,
        id: 1,
        inherited: false,
        name: 'test',
        published: true,
        samples: [],
        weight: 100,
      },
    ];

    render(
      <CategoryPatternsTable
        patterns={patterns}
        onUpdate={jest.fn()}
        onRemove={jest.fn()}
        onEdit={jest.fn()}
        onTogglePublished={jest.fn()}
      />
    );
  });
});
