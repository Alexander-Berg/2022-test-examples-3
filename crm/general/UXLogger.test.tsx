import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mocked } from 'ts-jest/utils';
import { logger } from 'services/Logger';
import { config } from 'services/Config';
import { UXLogger } from './UXLogger';
import { HOVER_THROTTLE, ALIVE_INTERVAL } from './UXLogger.config';
import { dispatchUXEvent } from './UXLogger.utils';
import { uxLogStub } from './UXLogger.stubs';

jest.useFakeTimers('modern');

jest.mock('services/Logger');
const loggerMocked = mocked(logger);

describe('UXLogger', () => {
  beforeEach(() => {
    loggerMocked.reportInfo.mockClear();
  });

  describe('when features.newFrontendLogs equals true', () => {
    beforeEach(() => {
      config.value.features.newFrontendLogs = true;
    });

    it('is able to report logs', () => {
      render(<UXLogger />);

      expect(loggerMocked.reportInfo).toBeCalled();
    });

    describe('when page loads', () => {
      it("reports 'load' event", () => {
        render(<UXLogger />);

        expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(uxLogStub('load'));
      });
    });

    describe('when user hovers element', () => {
      it("reports 'pointerover' event with throttling", () => {
        render(
          <>
            <UXLogger />
            <div data-testid="elementToHover" />
          </>,
        );

        loggerMocked.reportInfo.mockClear();
        userEvent.hover(screen.getByTestId('elementToHover'));
        expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
          uxLogStub('pointerover', undefined, ['elementToHover']),
        );

        loggerMocked.reportInfo.mockClear();
        userEvent.hover(screen.getByTestId('elementToHover'));
        expect(loggerMocked.reportInfo).not.toBeCalled();

        jest.advanceTimersByTime(HOVER_THROTTLE);

        loggerMocked.reportInfo.mockClear();
        userEvent.hover(screen.getByTestId('elementToHover'));
        expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
          uxLogStub('pointerover', undefined, ['elementToHover']),
        );
      });
    });

    describe('when user clicks on element', () => {
      it("reports 'click' event", () => {
        render(
          <>
            <UXLogger />
            <div data-testid="elementToClick" />
          </>,
        );

        loggerMocked.reportInfo.mockClear();
        userEvent.click(screen.getByTestId('elementToClick'));
        expect(loggerMocked.reportInfo.mock.calls[1][0]).toStrictEqual(
          uxLogStub('click', undefined, ['elementToClick']),
        );
      });
    });

    it("reports 'custom' event", () => {
      const CUSTOMEVENT_NAME = 'customEvent';
      const CUSTOMEVENT_DETAILS = { key: 'value' };
      render(<UXLogger />);

      loggerMocked.reportInfo.mockClear();
      dispatchUXEvent(CUSTOMEVENT_NAME, { details: CUSTOMEVENT_DETAILS });
      expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
        uxLogStub(CUSTOMEVENT_NAME, CUSTOMEVENT_DETAILS),
      );
    });

    it("reports 'alive' event over interval", () => {
      render(<UXLogger />);

      loggerMocked.reportInfo.mockClear();

      expect(loggerMocked.reportInfo).not.toBeCalled();

      jest.advanceTimersByTime(ALIVE_INTERVAL);

      expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(uxLogStub('alive'));
    });
  });

  describe('when features.newFrontendLogs equals false', () => {
    beforeEach(() => {
      config.value.features.newFrontendLogs = false;
    });

    it('is not able to report logs', () => {
      render(<UXLogger />);

      expect(loggerMocked.reportInfo).not.toBeCalled();
    });
  });
});
