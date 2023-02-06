import React from 'react';
import { render, screen, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { FocusScope } from './FocusScope';
import { FocusNode } from './FocusNode';

describe('FocusScope', () => {
  describe('initial behavior', () => {
    afterAll(() => {
      cleanup();
    });

    it('renders', () => {
      render(
        <>
          <button data-testid="button" />
          <FocusScope data-testid="scope">
            <FocusNode>
              <button />
            </FocusNode>
            <FocusNode>
              <input type="text" />
            </FocusNode>
          </FocusScope>
          <input data-testid="input" />
        </>,
      );
    });

    it('tabs to button and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('button')).toHaveFocus();
    });

    it('tabs to scope and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('scope')).toHaveFocus();
    });

    it('tabs to input and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('input')).toHaveFocus();
    });
  });

  describe('behavior when entering the scope', () => {
    afterAll(() => {
      cleanup();
    });

    it('renders', () => {
      render(
        <FocusScope data-testid="scope">
          <FocusNode>
            <button data-testid="button" />
          </FocusNode>
          <FocusNode>
            <input data-testid="input" />
          </FocusNode>
        </FocusScope>,
      );
    });

    it('tabs to scope and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('scope')).toHaveFocus();
    });

    it('enters scope and focuses the button', () => {
      userEvent.type(screen.getByTestId('scope'), '{enter}');

      expect(screen.getByTestId('button')).toHaveFocus();
    });

    it('tabs to input and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('input')).toHaveFocus();
    });
  });

  describe('behavior when nested scopes', () => {
    afterAll(() => {
      cleanup();
    });

    it('renders', () => {
      render(
        <FocusScope data-testid="scope">
          <FocusNode>
            <button data-testid="button" />
          </FocusNode>
          <FocusScope data-testid="nested-scope">
            <FocusNode>
              <button data-testid="nested-button" />
            </FocusNode>
            <FocusNode>
              <input data-testid="nested-input" />
            </FocusNode>
          </FocusScope>
        </FocusScope>,
      );
    });

    it('tabs to scope and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('scope')).toHaveFocus();
    });

    it('enters scope and focuses the button', () => {
      userEvent.type(screen.getByTestId('scope'), '{enter}');

      expect(screen.getByTestId('button')).toHaveFocus();
    });

    it('tabs to nested scope and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('nested-scope')).toHaveFocus();
    });

    it('enters nested scope and focuses nested button', () => {
      userEvent.type(screen.getByTestId('nested-scope'), '{enter}');

      expect(screen.getByTestId('nested-button')).toHaveFocus();
    });

    it('tabs to nested input and focuses it', () => {
      userEvent.tab();

      expect(screen.getByTestId('nested-input')).toHaveFocus();
    });
  });
});
