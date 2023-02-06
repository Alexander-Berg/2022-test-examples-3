import React from 'react';
import { rest } from 'msw';
import { mocked } from 'ts-jest/utils';
import { setupServer } from 'msw/node';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { modal as createFormModal } from 'modules/issues/components/Forms/CreateForm';
import { ModalGrid } from './ModalGrid';
import { createNewOpportunity } from './ModalGrid.utils';

const server = setupServer(
  rest.get('/form', (req, res, ctx) => {
    return res(ctx.json([]));
  }),
);

jest.mock('modules/issues/components/Forms/CreateForm');
// eslint-disable-next-line @typescript-eslint/no-explicit-any
mocked(createFormModal.open).mockImplementation(({ onSubmit }: any) => {
  onSubmit();
});

jest.mock('./ModalGrid.utils');
// eslint-disable-next-line @typescript-eslint/no-explicit-any
mocked(createNewOpportunity).mockImplementation(() => Promise.resolve('1') as any);

describe('ModalGrid', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  it('creates new opportunity', () => {
    const fn = jest.fn();
    render(<ModalGrid provider="/form" visible selected={[]} onClose={fn} onSave={fn} />);

    const createBtn = screen.getByText('Создать сделку');
    userEvent.click(createBtn);

    expect(createFormModal.open).toBeCalled();
    expect(createNewOpportunity).toBeCalled();
  });
});
