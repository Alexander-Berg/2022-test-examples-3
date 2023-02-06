import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BubbleInput } from './BubbleInput';

describe('components/BubbleInput', () => {
  it('renders text', () => {
    render(<BubbleInput text="text" />);

    expect(screen.getByText('text')).toBeInTheDocument();
  });

  it('calls onTextChange', () => {
    const handleTextChange = jest.fn();
    render(<BubbleInput text="" onTextChange={handleTextChange} />);

    userEvent.type(screen.getByRole('textbox'), 'test');

    expect(handleTextChange).toBeCalledTimes(4);
    expect(handleTextChange).toBeCalledWith('test');
  });

  it('renders placeholder', () => {
    render(<BubbleInput placeholder="placeholder" />);

    expect(screen.getByPlaceholderText('placeholder')).toBeInTheDocument();
  });

  describe('when hasClear is true', () => {
    it('renders cross when text is lengthwise', () => {
      render(<BubbleInput hasClear text="test" />);

      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it("doesn't render cross when text is empty", () => {
      render(<BubbleInput hasClear text="" />);

      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
  });

  describe('when hasClear is false', () => {
    it("doesn't render cross when text is lengthwise", () => {
      render(<BubbleInput hasClear={false} text="test" />);

      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it("doesn't render cross when text is empty", () => {
      render(<BubbleInput hasClear={false} text="" />);

      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
  });

  it('sets tabIndex', () => {
    render(<BubbleInput tabIndex={10} />);

    expect(screen.getByRole('textbox')).toHaveAttribute('tabIndex', '10');
  });

  it('selects text when autoFocus is true', async () => {
    render(<BubbleInput text="text" autoFocus />);

    await waitFor(() => {
      const selection = window.getSelection()!;
      expect(selection.anchorNode).toBeInstanceOf(Text);
      expect(selection.isCollapsed).toBeFalsy();
    });
  });

  it('selects textbox when autoFocus is true and text is empty', async () => {
    render(<BubbleInput text="" autoFocus />);

    const textbox = screen.getByRole('textbox');

    await waitFor(() => {
      const selection = window.getSelection()!;
      expect(selection.anchorNode).toBe(textbox);
      expect(selection.isCollapsed).toBeTruthy();
    });
  });

  it("doesn't select text when autoFocus is false", () => {
    render(<BubbleInput text="text" autoFocus={false} />);

    const selection = window.getSelection()!;
    const textbox = screen.getByRole('textbox');

    expect(selection.anchorNode).not.toBe(textbox);
  });

  it('calls innerRef', () => {
    const handleInnerRef = jest.fn();
    render(<BubbleInput innerRef={handleInnerRef} />);

    expect(handleInnerRef.mock.calls[0][0]).toBeInstanceOf(HTMLElement);
  });

  it('calls contentEditableRef', () => {
    const handleContentEditableRef = jest.fn();
    render(<BubbleInput contentEditableRef={handleContentEditableRef} />);

    expect(handleContentEditableRef.mock.calls[0][0]).toBeInstanceOf(HTMLElement);
  });
});
