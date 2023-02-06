import React from 'react';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import { SupportChatWidget } from './SupportChatWidget';
import { GUID_EMPTY_MESSAGE } from './SupportChatWidget.constants';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare let window: any;

const widget = {
  addListener: jest.fn(),
  removeListener: jest.fn(),
  isChatVisible: false,
  showChat: jest.fn(),
};

describe('SupportWidget', () => {
  beforeEach(() => {
    window.Ya = {
      ChatWidget: jest.fn().mockImplementation(() => {
        return widget;
      }),
    };
    window.chatWidget = undefined;
  });

  afterEach(() => {
    jest.clearAllMocks();
    cleanup();
  });

  it('renders component', async () => {
    render(<SupportChatWidget guid="someGuid" />);
    expect(screen.getByTestId('show-widget-button')).toBeInTheDocument();
  });
  describe('when widget init failed', () => {
    it(`throws error`, async () => {
      window.Ya = {};
      const rendered = () => render(<SupportChatWidget guid="someGuid" />);
      expect(rendered).toThrow();
    });
  });
  describe('when clicks button', () => {
    it('calls widget.showChat', async () => {
      const validators = [() => true];
      // eslint-disable-next-line react/jsx-no-bind
      render(<SupportChatWidget guid="someGuid" validators={validators} />);
      fireEvent.click(screen.getByTestId('show-widget-button'));
      expect(widget.showChat).toBeCalled();
    });
    describe('when GUID not valid', () => {
      it('shows error message ', async () => {
        // @ts-ignore
        render(<SupportChatWidget />);
        fireEvent.click(screen.getByTestId('show-widget-button'));
        expect(screen.getByText(GUID_EMPTY_MESSAGE)).toBeInTheDocument();
      });
    });
    describe('when validate failed', () => {
      it('shows error message', async () => {
        const errorMessage = 'test error message';
        const validators = [() => errorMessage];
        // eslint-disable-next-line react/jsx-no-bind
        render(<SupportChatWidget guid="someGuid" validators={validators} />);
        fireEvent.click(screen.getByTestId('show-widget-button'));
        expect(screen.getByText(errorMessage)).toBeInTheDocument();
      });
    });
  });
});
