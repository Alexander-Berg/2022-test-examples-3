import React from 'react';
import { render, screen } from '@testing-library/react';

import { LinkRenderer, ValueOfLink } from './LinkRenderer';

describe('<HeaderLinkRenderer/>', () => {
  it('renders without errors', () => {
    render(<LinkRenderer value={{} as ValueOfLink} />);
  });

  it('renders without label', () => {
    render(<LinkRenderer value={{ href: 'qwe' }} />);
    const linkElement = screen.queryByText('qwe');
    expect(linkElement).toBeDefined();
  });

  it('renders complete value', () => {
    render(<LinkRenderer value={{ label: '', href: 'qwe' }} />);
    expect(screen.queryByText('qwe')).toBeNull();
  });
});
