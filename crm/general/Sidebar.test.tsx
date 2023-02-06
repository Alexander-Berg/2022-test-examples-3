import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { Subject } from 'rxjs';
import { FetchDatesResponse } from '../Sidebar/Sidebar.types';

const todayDate = new Date();
todayDate.setMonth(2);
todayDate.setDate(5);
let calendarDatesRequestCount = 0;

const server = setupServer(
  rest.get('/manager-feed/calendar/dates', (req, res, ctx) => {
    calendarDatesRequestCount++;
    return res(
      ctx.json<FetchDatesResponse>({
        dates: [
          {
            date: todayDate.toISOString(),
            hasExpiredActivities: true,
          },
        ],
      }),
    );
  }),
);

describe('Sidebar', () => {
  let testSubject = new Subject();
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  beforeEach(() => {
    calendarDatesRequestCount = 0;
    testSubject = new Subject();
    jest.doMock('../RefreshContext', () => ({
      useRefreshSubject: () => testSubject,
    }));
  });

  it('renders datepicker with days with expired activities', async () => {
    const { Sidebar } = require('./Sidebar');
    render(<Sidebar date={todayDate} onDateChange={() => {}} />);
    const todayDatepickerDay = screen.getByText(todayDate.getDate());
    expect(todayDatepickerDay).toBeInTheDocument();
    expect(todayDatepickerDay).not.toHaveClass('Sidebar__dayHasExpiredTodos');

    await waitFor(() => {
      expect(todayDatepickerDay).toHaveClass('Sidebar__dayHasExpiredTodos');
    });
  });

  it('reloads datepicker on refresh subject', async () => {
    const { Sidebar } = require('./Sidebar');
    render(<Sidebar date={todayDate} onDateChange={() => {}} />);

    await waitFor(() => {
      expect(calendarDatesRequestCount).toBe(1);
    });

    testSubject.next();

    await waitFor(() => {
      expect(calendarDatesRequestCount).toBe(2);
    });
  });
});
