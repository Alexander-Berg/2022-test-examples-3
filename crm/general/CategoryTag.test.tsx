import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CategoryTag } from './CategoryTag';

describe('CategoryTag', () => {
  it('renders tag', async () => {
    render(<CategoryTag>Test</CategoryTag>);

    screen.getByRole('listitem');
  });

  describe('on click', () => {
    beforeEach(() => {
      render(<CategoryTag>Test</CategoryTag>);

      fireEvent.click(screen.getByRole('listitem'));
    });

    it('renders tooltip', async () => {
      await waitFor(() => {
        screen.getByRole('tooltip', { name: 'Test' });
      });
    });
  });
});
