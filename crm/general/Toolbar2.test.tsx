import React from 'react';
import { render } from '@testing-library/react';
import { Toolbar2 } from './Toolbar2';

describe('Toolbar2', () => {
  it('renders without errors', () => {
    const { container } = render(<Toolbar2 />);

    expect(container).toMatchSnapshot();
  });

  it('passes div props', () => {
    const { container } = render(<Toolbar2 className="className" />);

    expect(container).toMatchSnapshot();
  });

  it('renders only left', () => {
    const { container } = render(<Toolbar2 left="left" />);

    expect(container).toMatchSnapshot();
  });

  it('renders only right', () => {
    const { container } = render(<Toolbar2 right="right" />);

    expect(container).toMatchSnapshot();
  });

  it('renders both', () => {
    const { container } = render(<Toolbar2 left="left" right="right" />);

    expect(container).toMatchSnapshot();
  });
});
