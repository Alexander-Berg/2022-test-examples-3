import React from 'react';
import { render, screen } from '@testing-library/react';

import { GlobalCategoryParameter } from 'src/java/definitions';
import { GlobalNameRenderer } from './GlobalNameRenderer';

describe('<GlobalNameRenderer/>', () => {
  it('renders without errors', () => {
    render(<GlobalNameRenderer />);
  });

  it('renders without label', () => {
    render(<GlobalNameRenderer value={{ parameter: { paramId: 15 } as GlobalCategoryParameter }} />);
    const linkElement = screen.queryByText('/ui/global-params/edit?parameterId=15');
    expect(linkElement).toBeDefined();
  });

  it('renders complete value', () => {
    render(<GlobalNameRenderer value={{ parameter: { name: 'test', paramId: 15 } as GlobalCategoryParameter }} />);
    expect(screen.queryByText('/ui/global-params/edit?parameterId=15')).toBeNull();
  });
});
