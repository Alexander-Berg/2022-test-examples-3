import React from 'react';
import { render, waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { Subject } from 'rxjs';
import { setupServer } from 'msw/node';
import { TestBed } from 'components/TestBed';

let tableRequestCount = 0;

const server = setupServer(
  rest.get('/manager-feed/activity/list', (req, res, ctx) => {
    tableRequestCount++;
    return res(
      ctx.json({
        meta: {
          createAction: {
            access: 3,
            defaultFields: [],
          },
          fieldsVisibility: [],
          fields: [],
        },
        data: [],
      }),
    );
  }),
);

describe('MeetingsPage', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  beforeEach(() => {
    tableRequestCount = 0;
  });

  it('reloads table on refresh subject', async () => {
    const testSubject = new Subject();
    jest.doMock('./RefreshContext', () => ({
      useRefreshSubject: () => testSubject,
    }));
    const { MeetingsPage } = require('./MeetingsPage');

    render(
      <TestBed>
        <MeetingsPage />
      </TestBed>,
    );

    await waitFor(() => {
      expect(tableRequestCount).toBe(1);
    });

    testSubject.next();

    await waitFor(() => {
      expect(tableRequestCount).toBe(2);
    });
  });
});
