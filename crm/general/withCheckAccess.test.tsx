import React from 'react';
import { render, waitFor, screen, cleanup } from '@testing-library/react/pure';
import { withCheckAccess } from './withCheckAccess';

const TestComponent = withCheckAccess(() => <div>content</div>);

describe('withCheckAccess', () => {
  afterEach(() => {
    cleanup();
  });
  describe('when access IsEdit', () => {
    it(`renders`, async () => {
      render(<TestComponent access={3} />);
      await waitFor(() => screen.queryByText('content'));
    });
  });
  describe('when access IsRead', () => {
    it(`doesn't render`, async () => {
      render(<TestComponent access={1} />);
      expect(screen.queryByText('content')).toEqual(null);
    });
  });
});
