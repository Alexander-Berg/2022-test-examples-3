import React, { useState } from 'react';
import { addHours } from 'date-fns';
import { render, screen, waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { View } from './Sidebar/Sidebar.types';
import { Calendar } from './Calendar';
import { CalendarEvent } from '../MeetingsPage.types';

const todayDate = new Date();
HTMLElement.prototype.scrollIntoView = jest.fn();
jest.mock('../RefreshContext');

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

const TodayTestCalendar = () => {
  const [date, setDate] = useState(todayDate);
  const [view, setView] = useState<View>('day');

  return (
    <Calendar calendarView={view} onCalendarView={setView} date={date} onDateChange={setDate} />
  );
};

describe('Calendar', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  it('renders activities table', async () => {
    render(<TodayTestCalendar />);

    await waitFor(() => {
      expect(screen.getByText('event title')).toBeInTheDocument();
    });
  });

  it('renders view buttons', async () => {
    render(<TodayTestCalendar />);

    expect(screen.getByText('День')).toBeInTheDocument();
    expect(screen.getByText('Неделя')).toBeInTheDocument();
    expect(screen.getByText('Месяц')).toBeInTheDocument();
  });
});
