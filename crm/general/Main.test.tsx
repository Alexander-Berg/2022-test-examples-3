import React from 'react';
import { addHours } from 'date-fns';
import { render, waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { Subject } from 'rxjs';
import { setupServer } from 'msw/node';
import { CalendarEvent } from 'modules/meetings/MeetingsPage.types';

const todayDate = new Date();
let calendarRequestCount = 0;
HTMLElement.prototype.scrollIntoView = jest.fn();

const server = setupServer(
  rest.get('/manager-feed/activity/calendar', (req, res, ctx) => {
    calendarRequestCount++;
    return res(
      ctx.json<{
        items: CalendarEvent[];
      }>({
        items: [
          {
            id: '1',
            title: 'event title',
            fromDate: todayDate.toISOString(),
            toDate: addHours(todayDate, 2).toISOString(),
            editingUrl: '',
          },
        ],
      }),
    );
  }),
);

describe('Todos/Main', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  beforeEach(() => {
    calendarRequestCount = 0;
  });

  it('reloads calendar on refresh subject', async () => {
    const testSubject = new Subject();
    jest.doMock('../../RefreshContext', () => ({
      useRefreshSubject: () => testSubject,
    }));
    const { Main } = require('./Main');

    render(<Main date={todayDate} />);

    await waitFor(() => {
      expect(calendarRequestCount).toBe(1);
    });

    testSubject.next();

    await waitFor(() => {
      expect(calendarRequestCount).toBe(2);
    });
  });
});
