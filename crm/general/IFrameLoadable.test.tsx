import React from 'react';
import { render, waitFor, screen, cleanup } from '@testing-library/react/pure';
import { setupServer } from 'msw/node';
import { handlers } from './mocks/handlers';
import { IFrameLoadable } from './IFrameLoadable';

const server = setupServer(...handlers);

describe('IFrameLoadable', () => {
  beforeAll(() => {
    server.listen();
    server.resetHandlers();

    render(<IFrameLoadable url="/load" />);

    return waitFor(() => screen.findByTestId('iframe-loadable'));
  });

  afterAll(() => {
    server.close();
    cleanup();
  });

  it('has correct src', () => {
    expect((screen.getByTestId('iframe-loadable') as HTMLIFrameElement).src).toBe(
      'http://localhost/iframe',
    );
  });
});
