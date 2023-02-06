import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { AttachmentItem } from '.';

describe('AttachmentItem', () => {
  it('renders name', () => {
    const name = 'filename.extension';
    const { getByTestId } = render(<AttachmentItem name={name} />);
    expect(getByTestId('attach-item-file-name')).toHaveTextContent(name);
  });
  it('renders pdf extension', () => {
    const { getByTestId } = render(<AttachmentItem extension={'pdf'} />);
    expect(getByTestId('attach-item-badge-text')).toHaveTextContent('pdf');
    expect(getByTestId('attach-item-badge-text')).toHaveStyle({
      backgroundColor: '#E41E0C',
    });
  });
  it('renders zip extension', () => {
    const { getByTestId } = render(<AttachmentItem extension={'zip'} />);
    expect(getByTestId('attach-item-badge-text')).toHaveTextContent('zip');
    expect(getByTestId('attach-item-badge-text')).toHaveStyle({
      backgroundColor: '#144C5D',
    });
  });
  it('renders doc extension', () => {
    const { getByTestId } = render(<AttachmentItem extension={'doc'} />);
    expect(getByTestId('attach-item-badge-text')).toHaveTextContent('doc');
    expect(getByTestId('attach-item-badge-text')).toHaveStyle({
      backgroundColor: '#363CC9',
    });
  });
  it('renders xls extension', () => {
    const { getByTestId } = render(<AttachmentItem extension={'xls'} />);
    expect(getByTestId('attach-item-badge-text')).toHaveTextContent('xls');
    expect(getByTestId('attach-item-badge-text')).toHaveStyle({
      backgroundColor: '#0D7610',
    });
  });
  it('renders other extension', () => {
    const { getByTestId } = render(<AttachmentItem extension={'kek'} />);
    expect(getByTestId('attach-item-badge-text')).toHaveTextContent('kek');
    expect(getByTestId('attach-item-badge-text')).toHaveStyle({
      backgroundColor: '#5C5A57',
    });
  });
  it('renders shadow in extension', () => {
    const { getByTestId } = render(<AttachmentItem extension={'pdf'} shadow />);
    expect(getByTestId('attach-item-badge-shadow')).toBeInTheDocument();
    expect(getByTestId('attach-item-badge-text')).toBeInTheDocument();
    expect(getByTestId('attach-item-badge-text')).toHaveTextContent('pdf');
    expect(getByTestId('attach-item-badge-text')).toHaveStyle({ backgroundColor: '#E41E0C' });
  });
  it('renders background-image', () => {
    const { getByTestId } = render(<AttachmentItem previewImageUrl="test.image" />);
    expect(getByTestId('attach-item')).toHaveStyle({
      backgroundImage: 'url(test.image)',
    });
  });
  it('renders download link', () => {
    const { getByTestId } = render(<AttachmentItem downloadUrl="test.link" />);
    expect(getByTestId('attach-item-download-link')).toBeInTheDocument();
    expect(getByTestId('attach-item-download-link').getAttribute('href')).toBe('test.link');
  });

  describe('when clicked', () => {
    it('triggers onClick', () => {
      const handleClick = jest.fn();
      const { getByTestId } = render(<AttachmentItem onClick={handleClick} />);
      expect(fireEvent.click(getByTestId('attach-item'))).toBe(true);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('when deleted', () => {
    it('triggers onDelete', () => {
      const handleClick = jest.fn();
      const { getByTestId } = render(
        <AttachmentItem name={''} onClick={handleClick} onDelete={handleClick} />,
      );
      expect(fireEvent.click(getByTestId('attach-item-delete-button'))).toBe(true);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });
});
