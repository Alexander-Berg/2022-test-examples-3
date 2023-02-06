import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TestBed } from 'components/TestBed';
import { EventWidget } from './EventWidget';
import { Participant } from './EventWidget.types';

const participants: Participant[] = [
  {
    name: 'Андрей Ульянов',
    events: [
      {
        id: 1,
        range: {
          start: new Date('2021-06-03T08:10:00'),
          end: new Date('2021-06-03T12:30:00'),
        },
      },
      {
        id: 2,
        range: {
          start: new Date('2021-06-03T12:30:00'),
          end: new Date('2021-06-03T16:00:00'),
        },
      },
    ],
  },
  {
    name: 'Евгений Хромков',
    events: [
      {
        id: 3,
        range: {
          start: new Date('2021-06-03T12:00:00'),
          end: new Date('2021-06-03T12:30:00'),
        },
      },
    ],
  },
];

describe('EventWidget', () => {
  describe('props.date', () => {
    describe('when defined', () => {
      it('renders date', () => {
        render(
          <TestBed>
            <EventWidget date={new Date('2021-06-03T09:30:00')} />
          </TestBed>,
        );

        expect(screen.getByText('Thursday, June 3')).toBeInTheDocument();
      });

      describe('when is today date', () => {
        it('renders TimeMarker', () => {
          render(
            <TestBed>
              <EventWidget date={new Date()} />
            </TestBed>,
          );

          expect(screen.queryByTestId('timemarker')).toBeInTheDocument();
        });
      });

      describe("when isn't today date", () => {
        it("doesn't render TimeMarker", () => {
          render(
            <TestBed>
              <EventWidget date={new Date('2021-06-03T09:30:00')} />
            </TestBed>,
          );

          expect(screen.queryByTestId('timemarker')).not.toBeInTheDocument();
        });
      });
    });

    describe('when undefined', () => {
      it("doesn't render date", () => {
        render(
          <TestBed>
            <EventWidget />
          </TestBed>,
        );

        expect(screen.getByTestId('date-title').textContent).toBe('');
      });
    });
  });

  describe('props.viewRange', () => {
    describe('when defined', () => {
      it('renders hours', () => {
        render(
          <TestBed>
            <EventWidget viewRange={{ start: 0, end: 24 }} />
          </TestBed>,
        );

        expect(screen.getByTestId('hour-headers').childElementCount).toBe(24);
        expect(screen.getByTestId('hours').childElementCount).toBe(24);
      });
    });

    describe('when undefined', () => {
      it('renders default hours', () => {
        render(
          <TestBed>
            <EventWidget />
          </TestBed>,
        );

        expect(screen.getByTestId('hour-headers').childElementCount).toBe(16);
        expect(screen.getByTestId('hours').childElementCount).toBe(16);
      });
    });
  });

  describe('props.participants', () => {
    describe('when defined', () => {
      it('renders participants', () => {
        render(
          <TestBed>
            <EventWidget participants={participants} />
          </TestBed>,
        );

        expect(screen.getByTestId('timelines').childElementCount).toBe(2);
      });
    });

    describe('when undefined', () => {
      it("doesn't render participants", () => {
        render(
          <TestBed>
            <EventWidget />
          </TestBed>,
        );

        expect(screen.getByTestId('timelines').childElementCount).toBe(0);
      });
    });
  });

  describe('props.bookingRange', () => {
    describe('when defined', () => {
      it('renders BookingRange', () => {
        render(
          <TestBed>
            <EventWidget
              bookingRange={{
                start: new Date('2021-06-03T09:30:00'),
                end: new Date('2021-06-03T11:30:00'),
              }}
            />
          </TestBed>,
        );

        expect(screen.getByTestId('booking-range')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render BookingRange", () => {
        render(
          <TestBed>
            <EventWidget />
          </TestBed>,
        );

        expect(screen.queryByTestId('booking-range')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onNavigate', () => {
    it(`calls on click`, () => {
      const navigateCallback = jest.fn();
      render(
        <TestBed>
          <EventWidget date={new Date('2021-06-03T09:30:00')} onNavigate={navigateCallback} />
        </TestBed>,
      );

      userEvent.click(screen.getByTestId('button-previous-date'));
      expect(navigateCallback).toBeCalled();

      userEvent.click(screen.getByTestId('button-next-date'));
      expect(navigateCallback).toBeCalled();
    });
  });
});
