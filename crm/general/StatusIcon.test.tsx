import React from 'react';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TimelineItemStatus } from 'types/TimelineItemStatus';
import { StatusIcon } from './StatusIcon';
import { TOOLTIP_OPENDELAY } from './StatusIcon.config';

jest.useFakeTimers('modern');

const tooltipTextStub = 'tootip text';
const actionStub = {
  key: 'stub',
  onClick: jest.fn(),
  children: 'action text',
};

describe('StatusIcon', () => {
  beforeEach(() => {
    actionStub.onClick.mockClear();
  });
  describe('props.tooltip', () => {
    describe('when defined', () => {
      it('shows tooltip on icon hover', () => {
        render(<StatusIcon status={TimelineItemStatus.Sent} tooltip={tooltipTextStub} />);

        userEvent.hover(screen.getByTestId('status-icon'));

        act(() => {
          jest.advanceTimersByTime(TOOLTIP_OPENDELAY);
        });

        expect(screen.getByRole('tooltip')).toBeInTheDocument();
        expect(screen.getByText(tooltipTextStub)).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't show tooltip on icon hover", () => {
        render(<StatusIcon status={TimelineItemStatus.Sent} />);

        userEvent.hover(screen.getByTestId('status-icon'));

        act(() => {
          jest.advanceTimersByTime(TOOLTIP_OPENDELAY);
        });

        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.actions', () => {
    describe('when defined', () => {
      it('shows popup on icon click and hides on action click', () => {
        render(<StatusIcon status={TimelineItemStatus.Sent} actions={[actionStub]} />);

        userEvent.click(screen.getByTestId('status-icon'));

        expect(screen.getByTestId('status-icon-popup')).toBeInTheDocument();

        userEvent.click(screen.getByText(actionStub.children));

        expect(actionStub.onClick).toBeCalled();

        expect(screen.queryByText('status-icon-popup')).not.toBeInTheDocument();
      });

      it('hides tooltip on icon click', () => {
        render(
          <StatusIcon
            status={TimelineItemStatus.Sent}
            tooltip={tooltipTextStub}
            actions={[actionStub]}
          />,
        );

        userEvent.hover(screen.getByTestId('status-icon'));

        act(() => {
          jest.advanceTimersByTime(TOOLTIP_OPENDELAY);
        });
        expect(screen.getByText(tooltipTextStub)).toBeInTheDocument();

        userEvent.click(screen.getByTestId('status-icon'));

        expect(screen.queryByText(tooltipTextStub)).not.toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't show popup on icon click", () => {
        render(<StatusIcon status={TimelineItemStatus.Sent} />);

        userEvent.click(screen.getByTestId('status-icon'));

        expect(screen.queryByText('status-icon-popup')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.status', () => {
    describe('when defined', () => {
      it('renders icon', () => {
        render(<StatusIcon status={TimelineItemStatus.Sent} />);

        expect(screen.getByTestId('status-icon')).toBeInTheDocument();
      });
    });

    describe("when equals 'failed'", () => {
      it('renders error icon', () => {
        render(<StatusIcon status={TimelineItemStatus.Failed} />);

        expect(screen.getByTestId('status-icon')).toHaveClass('StatusIcon_error');
      });
    });

    describe('when changed', () => {
      it('hides popup', () => {
        const { rerender } = render(
          <StatusIcon status={TimelineItemStatus.Failed} actions={[actionStub]} />,
        );

        userEvent.click(screen.getByTestId('status-icon'));

        expect(screen.getByTestId('status-icon-popup')).toBeInTheDocument();

        rerender(<StatusIcon status={TimelineItemStatus.Sent} />);

        expect(screen.queryByText('status-icon-popup')).not.toBeInTheDocument();
      });
    });
  });
});
