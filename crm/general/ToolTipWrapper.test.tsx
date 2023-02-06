import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ToolTipWrapper } from './ToolTipWrapper';

jest.useFakeTimers();

describe('ToolTipWrapper', () => {
  describe('props.openDelay', () => {
    describe('when defined', () => {
      it('opens popup with delay', () => {
        const openDelay = 500;
        render(
          <ToolTipWrapper openDelay={openDelay} content="content">
            <div>wrappedComponent</div>
          </ToolTipWrapper>,
        );

        expect(screen.queryByText('content')).not.toBeInTheDocument();

        userEvent.hover(screen.getByText('wrappedComponent'));

        expect(screen.queryByText('content')).not.toBeInTheDocument();

        jest.advanceTimersByTime(openDelay);

        expect(screen.getByText('content')).toBeInTheDocument();
      });
    });
  });

  describe('props.closeDelay', () => {
    describe('when defined', () => {
      it('closes popup with delay', () => {
        const closeDelay = 500;
        render(
          <ToolTipWrapper closeDelay={closeDelay} content="content">
            <div>wrappedComponent</div>
          </ToolTipWrapper>,
        );

        userEvent.hover(screen.getByText('wrappedComponent'));
        jest.advanceTimersByTime(0);
        expect(screen.getByText('content')).toBeInTheDocument();

        userEvent.unhover(screen.getByText('wrappedComponent'));
        expect(screen.getByText('content')).toBeInTheDocument();

        jest.advanceTimersByTime(closeDelay);

        expect(screen.queryByText('content')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.content', () => {
    describe('when defined', () => {
      it('shows content on mouse enter', () => {
        render(
          <ToolTipWrapper content="content">
            <div>wrappedComponent</div>
          </ToolTipWrapper>,
        );

        expect(screen.queryByText('content')).not.toBeInTheDocument();

        userEvent.hover(screen.getByText('wrappedComponent'));
        jest.advanceTimersByTime(0);

        expect(screen.getByText('content')).toBeInTheDocument();
      });

      it('hides content on mouse leave', () => {
        render(
          <ToolTipWrapper content="content">
            <div>wrappedComponent</div>
          </ToolTipWrapper>,
        );

        userEvent.hover(screen.getByText('wrappedComponent'));
        jest.advanceTimersByTime(0);

        expect(screen.getByText('content')).toBeInTheDocument();

        userEvent.unhover(screen.getByText('wrappedComponent'));
        jest.advanceTimersByTime(0);

        expect(screen.queryByText('content')).not.toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't show content on mouse enter", () => {
        render(
          <ToolTipWrapper>
            <div>wrappedComponent</div>
          </ToolTipWrapper>,
        );

        expect(screen.queryByText('content')).not.toBeInTheDocument();

        userEvent.hover(screen.getByText('wrappedComponent'));

        expect(screen.queryByText('content')).not.toBeInTheDocument();
      });
    });
  });
});
