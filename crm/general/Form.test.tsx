import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Form } from './Form';

describe('Form', () => {
  describe('when there is no text', () => {
    it('disables the send button', () => {
      render(<Form onSubmit={jest.fn()} />);

      const button = screen.getByRole('button', { name: 'Отправить' }) as HTMLButtonElement;

      expect(button.disabled).toBe(true);
    });
  });

  describe('when there is some text', () => {
    it(`doesn't disable the send button`, () => {
      render(<Form onSubmit={jest.fn()} />);
      const textarea = screen.getByRole('textbox');
      fireEvent.change(textarea, { target: { value: 'some text' } });

      const button = screen.getByRole('button', { name: 'Отправить' }) as HTMLButtonElement;

      expect(button.disabled).toBe(false);
    });
  });

  describe('on send button click', () => {
    it('calls props.onSubmit', () => {
      const mockOnSubmit = jest.fn();
      render(<Form onSubmit={mockOnSubmit} />);
      const textarea = screen.getByRole('textbox');
      fireEvent.change(textarea, { target: { value: 'some text' } });
      const button = screen.getByRole('button', { name: 'Отправить' }) as HTMLButtonElement;
      fireEvent.click(button);

      expect(mockOnSubmit).toBeCalledWith('some text');
    });
  });
});
