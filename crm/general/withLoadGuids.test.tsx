import React from 'react';
import { render, waitFor, screen, cleanup } from '@testing-library/react/pure';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { withLoadGuids } from './withLoadGuids';

const server = setupServer(
  rest.get(`${window.CRM_SPACE_API_HOST}/supportChatError`, (req, res, ctx) => {
    return res(ctx.status(500), ctx.json({ Message: 'Error' }));
  }),
  rest.get(`${window.CRM_SPACE_API_HOST}/supportChat`, (req, res, ctx) => {
    return res(ctx.json({ Mail: 'MailGuid', Chat: 'ChatGuid' }));
  }),
);

const TestComponent = withLoadGuids((props) => {
  if (props.guid) {
    return <div>{props.guid}</div>;
  }

  return <div>Пустой GUID</div>;
});

describe('withLoadGuids', () => {
  beforeAll(() => {
    server.listen();
    server.resetHandlers();
  });

  afterAll(() => {
    server.close();
  });

  afterEach(() => {
    cleanup();
  });
  describe('when fetching failed', () => {
    it(`doesn't render`, async () => {
      render(<TestComponent comType="Mail" url="/supportChatError" />);
      const rendered = await waitFor(() => screen.queryByText('MailGuid'));
      expect(rendered).toEqual(null);
    });
  });
  describe('when fetching success', () => {
    it('renders component', async () => {
      render(<TestComponent comType="Mail" />);
      return waitFor(() => screen.findAllByText('MailGuid'));
    });
  });
});
