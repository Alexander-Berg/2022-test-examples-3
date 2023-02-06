import React from 'react';
import { render, screen, cleanup } from '@testing-library/react/pure';
import { TagCell } from './TagCell';
import { TagCellProps } from './TagCell.types';

const mockedProps: TagCellProps = {
  cell: {
    id: '1',
    type: 'Tag',
    data: { id: '1', name: 'TestTagName', color: 'ff0' },
  },
};

const mockedPropsWithLinks: TagCellProps = {
  cell: {
    id: '1',
    type: 'Tag',
    data: { id: '1', name: 'TestTagName', color: 'ff0', link: '/somePath' },
  },
};

describe('TagCell', () => {
  afterEach(() => {
    cleanup();
  });
  it('renders', () => {
    render(<TagCell {...mockedProps} />);
    expect(screen.queryByText('TestTagName')).toBeInTheDocument();
  });
  it('generates correct link', () => {
    const { container } = render(<TagCell {...mockedPropsWithLinks} />);
    expect(container.querySelector(`a[href$='/somePath']`)).toBeInTheDocument();
  });
});
