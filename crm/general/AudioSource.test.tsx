import React from 'react';
import userEvent from '@testing-library/user-event';
import { get } from 'api/common';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { render, waitFor, screen } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { playIconTestId } from 'components/design/Audio/PlayButton/PlayButton.constants';
import { AudioSource } from './AudioSource';

jest.mock('./MetaCache', () => ({
  MetaCache: class MetaCacheMock {
    static create() {
      return new MetaCacheMock();
    }

    get() {
      return null;
    }

    set() {}

    remove() {}
  },
}));

let metaRequestsCount = 0;
let recordRequestsCount = 0;
const server = setupServer(
  rest.get('/record', (req, res) => {
    recordRequestsCount++;
    return res();
  }),

  rest.get('/metaOk', (req, res, ctx) => {
    metaRequestsCount++;
    return res(ctx.status(200));
  }),

  rest.get('/metaNotReadyError', (req, res, ctx) => {
    metaRequestsCount++;
    return res(ctx.status(404), ctx.json('Запись еще не получена'));
  }),

  rest.get('/metaNeverExistsError', (req, res, ctx) => {
    metaRequestsCount++;
    return res(ctx.status(410), ctx.json('Never exists error'));
  }),

  rest.get('/metaInternalError', (req, res, ctx) => {
    metaRequestsCount++;

    return res(ctx.status(500), ctx.json('Internal error'));
  }),
);

describe('Audio', () => {
  beforeAll(() => {
    require('./setupJest');
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  beforeEach(() => {
    metaRequestsCount = 0;
    recordRequestsCount = 0;
  });

  describe('loadable mock', () => {
    beforeAll(() => {
      window.HTMLMediaElement.prototype.load = jest.fn(function() {
        get({
          url: this.src,
        }).then(() => {
          this.dispatchEvent(new Event('loadeddata'));
          this.dispatchEvent(new Event('canplaythrough'));
        });
      });
    });

    describe('when no metaSrc provided', () => {
      it('loads record', async () => {
        render(
          <TestBed>
            <AudioSource audio={new window.Audio()} src="/record" />
          </TestBed>,
        );

        userEvent.click(screen.getByTestId(playIconTestId));

        await waitFor(() => {
          expect(recordRequestsCount).toBe(1);
        });
      });
    });

    describe('when "OK" meta', () => {
      it('loads record', async () => {
        render(
          <TestBed>
            <AudioSource audio={new window.Audio()} metaSrc="/metaOk" src="/record" />
          </TestBed>,
        );

        await waitFor(() => {
          expect(metaRequestsCount).toBe(1);
        });

        userEvent.click(screen.getByTestId(playIconTestId));

        await waitFor(() => {
          expect(recordRequestsCount).toBe(1);
        });
      });
    });

    describe('when "Not ready error" meta', () => {
      it('renders error', async () => {
        render(
          <TestBed>
            <AudioSource audio={new window.Audio()} metaSrc="/metaNotReadyError" src="/record" />
          </TestBed>,
        );

        await waitFor(() => {
          expect(metaRequestsCount).toBe(1);
        });

        await waitFor(() => {
          expect(screen.getByText('Запись еще не получена')).toBeInTheDocument();
        });

        expect(recordRequestsCount).toBe(0);
      });
    });

    describe('when "Never exists error" meta', () => {
      it('renders error', async () => {
        render(
          <TestBed>
            <AudioSource audio={new window.Audio()} metaSrc="/metaNeverExistsError" src="/record" />
          </TestBed>,
        );

        await waitFor(() => {
          expect(metaRequestsCount).toBe(1);
        });

        await waitFor(() => {
          expect(screen.getByText('Never exists error')).toBeInTheDocument();
        });

        expect(recordRequestsCount).toBe(0);
      });
    });

    describe('when "Internal error" meta', () => {
      it('renders error', async () => {
        render(
          <TestBed>
            <AudioSource audio={new window.Audio()} metaSrc="/metaInternalError" src="/record" />
          </TestBed>,
        );

        await waitFor(() => {
          expect(metaRequestsCount).toBe(1);
        });

        await waitFor(() => {
          expect(screen.getByText('Internal error')).toBeInTheDocument();
        });

        expect(recordRequestsCount).toBe(0);
      });
    });

    describe('when keepPlayOnUnmount is true', () => {
      it('continues playing on unmount', async () => {
        const audio = new window.Audio();

        const { unmount } = render(
          <TestBed>
            <AudioSource audio={audio} metaSrc="/metaOk" src="/record" />
          </TestBed>,
        );

        await waitFor(() => {
          expect(metaRequestsCount).toBe(1);
        });

        userEvent.click(screen.getByTestId(playIconTestId));

        await waitFor(() => {
          expect(recordRequestsCount).toBe(1);
        });

        unmount();

        expect(audio.paused).toBe(false);
      });
    });

    describe('when keepPlayOnUnmount is false', () => {
      it('stops playing on unmount', async () => {
        const audio = new window.Audio();

        const { unmount } = render(
          <TestBed>
            <AudioSource audio={audio} metaSrc="/metaOk" src="/record" keepPlayOnUnmount={false} />
          </TestBed>,
        );

        await waitFor(() => {
          expect(metaRequestsCount).toBe(1);
        });

        userEvent.click(screen.getByTestId(playIconTestId));

        await waitFor(() => {
          expect(recordRequestsCount).toBe(1);
        });

        unmount();

        expect(audio.paused).toBe(true);
      });
    });
  });
});
