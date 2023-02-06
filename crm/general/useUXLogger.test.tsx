import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mocked } from 'ts-jest/utils';
import { logger } from 'services/Logger';
import { config } from 'services/Config';
import { UXEvent } from './useUXLogger.types';
import { useUXLogger } from './useUXLogger';
import { uxLogStub as uxLogStubBase } from '../UXLogger.stubs';

jest.useFakeTimers('modern');

jest.mock('services/Logger');
const loggerMocked = mocked(logger);

const TestComponent: React.FC<{ events: UXEvent[] }> = ({ events }) => {
  useUXLogger(events);

  return <div data-testid="testComponent" />;
};

const uxLogStub = (eventType, details?: unknown) =>
  uxLogStubBase(eventType, details, ['testComponent']);

describe('useUXLogger', () => {
  beforeEach(() => {
    config.value.features.newFrontendLogs = true;
    loggerMocked.reportInfo.mockClear();
  });

  describe('props.events', () => {
    describe('when defined', () => {
      it('listens and reports events', () => {
        const EVENT_DETAILS = { key: 'value' };
        const EVENTS: UXEvent[] = [{ type: 'click', details: () => EVENT_DETAILS }];
        render(<TestComponent events={EVENTS} />);

        userEvent.click(screen.getByTestId('testComponent'));

        expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
          uxLogStub('click', EVENT_DETAILS),
        );
      });

      it('listens and reports events with custom callback', () => {
        const callback = jest.fn();
        const EVENTS: UXEvent[] = [{ type: 'click', callback }];
        render(<TestComponent events={EVENTS} />);

        userEvent.click(screen.getByTestId('testComponent'));

        expect(loggerMocked.reportInfo).not.toBeCalled();
        expect(callback).toBeCalled();
      });

      describe('when unmount', () => {
        it('removes listeners', () => {
          const EVENTS: UXEvent[] = [{ type: 'click' }];
          const { rerender } = render(<TestComponent events={EVENTS} />);

          rerender(<div data-testid="testComponent" />);
          userEvent.click(screen.getByTestId('testComponent'));

          expect(loggerMocked.reportInfo).not.toBeCalled();
        });
      });
    });
  });
});
