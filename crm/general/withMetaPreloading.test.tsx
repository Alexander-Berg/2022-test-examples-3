import React, { FC, useEffect } from 'react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { get } from 'api/common';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { withMetaPreloading } from './withMetaPreloading';
import { AudioProps } from '../../Audio.types';
import { WithMetaPreloadingProps } from './withMetaPreloading.types';

const TestComponent: FC<WithMetaPreloadingProps<AudioProps>> = withMetaPreloading((props) => {
  const { src, error, onRetry } = props;
  const { message = 'error', canRetry = true } = error || {};

  useEffect(() => {
    if (src) {
      get({
        url: src,
      });
    }
  }, [src]);

  if (error) {
    return (
      <>
        <span>{message || 'error'}</span>
        {canRetry && <button onClick={onRetry}>retry</button>}
      </>
    );
  }

  return null;
});

let storage: {
  metaRequestsCount: number;
  recordRequestsCount: number;
};
const server = setupServer(
  rest.get('/record', (req, res) => {
    storage.recordRequestsCount++;
    return res();
  }),

  rest.get('/metaOk', (req, res, ctx) => {
    storage.metaRequestsCount++;
    return res(ctx.status(200));
  }),

  rest.get('/metaNotReadyError', (req, res, ctx) => {
    storage.metaRequestsCount++;
    return res(ctx.status(404), ctx.json('Not ready error'));
  }),

  rest.get('/metaNeverExistsError', (req, res, ctx) => {
    storage.metaRequestsCount++;
    return res(ctx.status(410), ctx.json('Never exists error'));
  }),

  rest.get('/metaInternalError3Retries', (req, res, ctx) => {
    storage.metaRequestsCount++;
    // successful retry
    if (storage.metaRequestsCount === 3) {
      return res();
    }

    return res(ctx.status(500), ctx.json('Internal error'));
  }),
);

describe('withMetaPreloading', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  beforeEach(() => {
    storage = {
      metaRequestsCount: 0,
      recordRequestsCount: 0,
    };
  });

  describe('when no metaSrc provided', () => {
    it('loads record', async () => {
      render(<TestComponent src="/record" />);

      await waitFor(() => {
        expect(storage.recordRequestsCount).toBe(1);
      });
    });
  });

  describe('when "OK" meta', () => {
    it('loads record', async () => {
      render(<TestComponent metaSrc="/metaOk" src="/record" />);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(1);
      });

      await waitFor(() => {
        expect(storage.recordRequestsCount).toBe(1);
      });
    });
  });

  describe('when "Not ready error" meta', () => {
    it('renders error with retry button', async () => {
      render(<TestComponent metaSrc="/metaNotReadyError" src="/record" />);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(1);
      });

      await waitFor(() => {
        expect(screen.getByText('Not ready error')).toBeInTheDocument();
        expect(screen.queryByText('retry')).toBeInTheDocument();
      });

      expect(storage.recordRequestsCount).toBe(0);
    });
  });

  describe('when "Never exists error" meta', () => {
    it('renders error without retry button', async () => {
      render(<TestComponent metaSrc="/metaNeverExistsError" src="/record" />);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(1);
      });

      await waitFor(() => {
        expect(screen.getByText('Never exists error')).toBeInTheDocument();
        expect(screen.queryByText('retry')).not.toBeInTheDocument();
      });

      expect(storage.recordRequestsCount).toBe(0);
    });
  });

  describe('when "Internal error" meta', () => {
    it('renders error with retry button', async () => {
      render(<TestComponent metaSrc="/metaInternalError3Retries" src="/record" />);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(1);
      });

      await waitFor(() => {
        expect(screen.getByText('Internal error')).toBeInTheDocument();
        expect(screen.getByText('retry')).toBeInTheDocument();
      });

      expect(storage.recordRequestsCount).toBe(0);
    });

    it('refetches meta on retry', async () => {
      render(<TestComponent metaSrc="/metaInternalError3Retries" src="/record" />);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(1);
      });

      let retryButton;
      await waitFor(() => {
        retryButton = screen.getByText('retry');
        expect(retryButton).toBeInTheDocument();
      });

      userEvent.click(retryButton);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(2);
      });

      expect(storage.recordRequestsCount).toBe(0);
    });
  });

  describe('when meta retry is successful', () => {
    it('loads record', async () => {
      render(<TestComponent metaSrc="/metaInternalError3Retries" src="/record" />);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(1);
      });

      let retryButton;
      await waitFor(() => {
        retryButton = screen.getByText('retry');
        expect(retryButton).toBeInTheDocument();
      });

      userEvent.click(retryButton);

      await waitFor(() => {
        expect(storage.metaRequestsCount).toBe(2);
      });

      await waitFor(() => {
        expect(screen.getByText('Internal error')).toBeInTheDocument();
      });

      expect(storage.recordRequestsCount).toBe(0);
      userEvent.click(retryButton);

      await waitFor(() => {
        expect(screen.queryByText('Internal error')).not.toBeInTheDocument();
        expect(screen.queryByText('retry')).not.toBeInTheDocument();
      });

      expect(storage.recordRequestsCount).toBe(1);
    });
  });
});
