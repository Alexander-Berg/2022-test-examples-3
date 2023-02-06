/* global describe, it, expect */
import React from 'react';
import renderer from 'react-test-renderer';
import { TestBed } from 'components/TestBed';
import CalendarEvent from '../CalendarEvent';

describe('Calendar Event', () => {
  it('should be null if no props', () => {
    const component = renderer.create(<CalendarEvent />);

    expect(component.toJSON()).toMatchSnapshot();
  });

  it('should be no render startTimeZone if startTimeZone === endTimeZone', () => {
    const event = {
      summary: 'Title',
      description: 'Description\n',
      startTime: '2017-11-15T17:30:00.0000000+03:00',
      startTimeZone: '(UTC+03:00) Moscow, St. Petersburg, Volgograd',
      endTime: '2017-11-15T18:30:00.0000000+03:00',
      endTimeZone: '(UTC+03:00) Moscow, St. Petersburg, Volgograd',
      organizer: {
        commonName: 'Тарасов Никита',
      },
      attendees: [
        {
          commonName: 'dcrm-support@yandex.ru',
        },
      ],
    };

    const component = renderer.create(
      <TestBed>
        <CalendarEvent event={event} />
      </TestBed>,
    );

    expect(component.toJSON()).toMatchSnapshot();
  });

  it('should be render startTimeZone if startTimeZone !== endTimeZone', () => {
    const event = {
      summary: 'Title',
      description: 'Description\n',
      startTime: '2017-11-15T17:30:00.0000000+03:00',
      startTimeZone: '(UTC+03:00) Moscow, St. Petersburg, Volgograd',
      endTime: '2017-11-15T18:30:00.0000000+03:00',
      endTimeZone: '(UTC+00:00) London',
      organizer: {
        commonName: 'Тарасов Никита',
      },
      attendees: [
        {
          commonName: 'dcrm-support@yandex.ru',
        },
      ],
    };

    const component = renderer.create(
      <TestBed>
        <CalendarEvent event={event} />
      </TestBed>,
    );

    expect(component.toJSON()).toMatchSnapshot();
  });
});
