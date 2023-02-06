import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TagCollection } from './TagCollection';

describe('TagCollection', () => {
  it('renders common list of tags', () => {
    render(<TagCollection common={[{ id: 1, text: 'common' }]} />);

    expect(screen.getByText('common')).toBeInTheDocument();
  });

  it('renders personal list of tags', () => {
    render(<TagCollection personal={[{ id: 1, text: 'personal' }]} />);

    expect(screen.getByText('Личные')).toBeInTheDocument();
    expect(screen.getByText('personal')).toBeInTheDocument();
  });

  it('renders creation button', () => {
    render(<TagCollection />);

    expect(screen.getByText('Создать метку')).toBeInTheDocument();
  });

  it('renders creation button with text', () => {
    render(<TagCollection creationText="test" />);

    expect(screen.getByText('Создать «test»')).toBeInTheDocument();
  });

  it('calls onCreate callback', () => {
    const handleCreate = jest.fn();
    render(<TagCollection creationText="test" onCreate={handleCreate} />);

    userEvent.click(screen.getByText('Создать «test»'));

    expect(handleCreate).toBeCalledWith('test');
  });

  it('calls onChange callback', () => {
    const handleChange = jest.fn();
    render(<TagCollection common={[{ id: 1, text: 'common' }]} onChange={handleChange} />);

    userEvent.click(screen.getByText('common'));

    expect(handleChange).toBeCalledWith(1);
  });
});
