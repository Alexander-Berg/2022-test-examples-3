import React from 'react';
import { render, screen, act, waitFor, cleanup, fireEvent } from '@testing-library/react/pure';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { delay } from 'utils/delay';
import { Form } from 'types/api/form/Form';
import { FormBySchemeLoadable } from './FormBySchemeLoadable';

const createSimpleFormScheme = (value = 'Text value'): Form => ({
  meta: {
    fieldsVisibility: ['Text'],
    fields: [
      {
        id: 'Text',
        type: 'Text',
        title: 'Text caption',
        access: 3,
        isFieldsUpdateNeeded: false,
      },
    ],
  },
  data: [
    {
      id: '1',
      fields: [
        {
          id: 'Text',
          type: 'Text',
          data: { value },
        },
      ],
    },
  ],
});

const server = setupServer(
  rest.get('/form', (req, res, ctx) => {
    return res(ctx.json(createSimpleFormScheme()));
  }),

  rest.post('/form', (req, res, ctx) => {
    // @ts-ignore
    return res(ctx.json(createSimpleFormScheme(String(req.body.fields[0].data.value)).data[0]));
  }),
);

beforeAll(() => {
  server.listen();
});
afterAll(() => {
  server.close();
});

describe('FormBySchemeLoadable', () => {
  describe('get form scheme with error', () => {
    afterEach(() => {
      cleanup();
    });

    it('renders error on server error', async () => {
      server.use(
        rest.get('/form', (req, res, ctx) => {
          return res.once(ctx.status(500), ctx.json({ message: 'Load form error' }));
        }),
      );

      await act(async () => {
        render(<FormBySchemeLoadable url="/form" />);
      });

      await waitFor(() => expect(screen.getByText(/Load form error/)).toBeInTheDocument());
    });

    it('renders error on wrong data', async () => {
      server.use(
        rest.get('/form', (req, res, ctx) => {
          return res.once(ctx.status(200), ctx.json({}));
        }),
      );

      await act(async () => {
        render(<FormBySchemeLoadable url="/form" />);
      });

      await waitFor(() => expect(screen.getByText(/Error:/)).toBeInTheDocument());
    });
  });

  describe('edit form process', () => {
    afterAll(() => {
      cleanup();
    });

    const getEditButton = () => screen.getByRole('button', { name: /редактировать/i });
    const getSaveButton = () => screen.getByRole('button', { name: /сохранить/i });
    const getInput = (text = 'Text value') => screen.getByDisplayValue(text);

    it('renders form', async () => {
      await act(async () => {
        render(<FormBySchemeLoadable url="/form" />);
      });

      await waitFor(() => expect(screen.getByText(/Text caption/)).toBeInTheDocument());
    });

    it('changes to edit mode', async () => {
      await act(async () => {
        fireEvent.click(getEditButton());
      });

      await waitFor(() => expect(getInput()).toBeInTheDocument());
    });

    it('saves form with backend error', async () => {
      server.use(
        rest.post('/form', (req, res, ctx) => {
          return res.once(ctx.status(500), ctx.json({}));
        }),
      );

      await act(async () => {
        fireEvent.change(getInput(), { target: { value: 'Text value 2' } });
      });

      await act(async () => {
        fireEvent.click(getSaveButton());
      });

      await delay(0);

      expect(getInput('Text value 2')).toBeInTheDocument();
    });

    it('saves success form data', async () => {
      await act(async () => {
        fireEvent.click(getSaveButton());
      });

      await waitFor(() => getEditButton());

      expect(screen.getByText(/Text value 2/)).toBeInTheDocument();
    });
  });
});
