import React from 'react';
import { render, waitFor, screen, cleanup } from '@testing-library/react/pure';
import { setupServer } from 'msw/node';
import { handlers } from './mocks/handlers';
import { withLoad } from './withLoad';
import { TestComponent } from './TestComponent';

const server = setupServer(...handlers);

const LoadableTestComponent = withLoad(TestComponent);

describe('withLoad', () => {
  beforeAll(() => {
    server.listen();
    server.resetHandlers();
  });

  afterAll(() => {
    server.close();
  });

  describe('success load', () => {
    beforeAll(() => {
      render(<LoadableTestComponent url="/load" />);

      return waitFor(() => screen.findAllByText('loaded'));
    });

    afterAll(() => {
      cleanup();
    });

    it('displays loaded data', () => {
      expect(screen.getByText('loaded')).toBeInTheDocument();
    });
  });

  describe('failed load', () => {
    beforeAll(() => {
      render(<LoadableTestComponent url="/error" />);

      return waitFor(() => screen.findAllByText(/При загрузке данных произошла ошибка/i));
    });

    afterAll(() => {
      cleanup();
    });

    it('displays loaded data', () => {
      expect(screen.getByText(/При загрузке данных произошла ошибка/i)).toBeInTheDocument();
    });
  });
});
