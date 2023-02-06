import React from 'react';
import { render, screen } from '@testing-library/react';

import { DisplayCategory, LocalCategoryParameter } from 'src/java/definitions';
import { CategoryNameRenderer } from './CategoryNameRenderer';

describe('<CategoryNameRenderer/>', () => {
  it('renders without errors', () => {
    render(<CategoryNameRenderer />);
  });

  it('renders without label', () => {
    render(
      <CategoryNameRenderer
        value={{ parameter: { paramId: 15 } as LocalCategoryParameter, category: { hid: 0 } as DisplayCategory }}
      />
    );
    const linkElement = screen.queryByText('/ui/category-params/edit?parameterId=15&hid=0');
    expect(linkElement).toBeDefined();
  });

  it('renders complete value', () => {
    render(
      <CategoryNameRenderer
        value={{
          parameter: { name: 'test', paramId: 15 } as LocalCategoryParameter,
          category: { hid: 0 } as DisplayCategory,
        }}
      />
    );
    expect(screen.queryByText('/ui/category-params/edit?parameterId=15&hid=0')).toBeNull();
  });
});
