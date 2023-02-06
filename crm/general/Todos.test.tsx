import React, { useState } from 'react';
import { addHours } from 'date-fns';
import { render, screen, waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { Todos } from './Todos';
import { CalendarEvent } from '../MeetingsPage.types';

const todayDate = new Date();
jest.mock('../RefreshContext');
HTMLElement.prototype.scrollIntoView = jest.fn();

const server = setupServer(
  rest.get('/manager-feed/activity/calendar', (req, res, ctx) =>
    res(
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
    ),
  ),
);

const TodayTestTodos = () => {
  const [date, setDate] = useState(todayDate);

  return <Todos date={date} onDateChange={setDate} />;
};

describe('Todos', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  it('renders calendar events', async () => {
    render(<TodayTestTodos />);

    await waitFor(() => {
      expect(screen.getByText('event title')).toBeInTheDocument();
    });
  });
});
