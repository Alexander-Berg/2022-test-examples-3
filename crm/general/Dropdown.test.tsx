import React, { ReactNode } from 'react';
import { render, fireEvent, waitFor, screen } from '@testing-library/react';
import Dropdown from './Dropdown';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

describe('Dropdown', () => {
  const content: ReactNode = <div>content</div>;

  describe('on init', () => {
    beforeEach(() => {
      render(<Dropdown content={content}>Button</Dropdown>);
    });

    it('does not show content', () => {
      expect(screen.queryByText('content')).not.toBeInTheDocument();
    });
  });

  describe('after click on button', () => {
    beforeEach(() => {
      render(<Dropdown content={content}>Button</Dropdown>);

      fireEvent.click(screen.getByText('Button'));
    });

    it('shows content', async () => {
      await waitFor(() => {
        expect(screen.queryByText('content')).toBeVisible();
      });
    });
  });

  describe('after second click on button', () => {
    beforeEach(async () => {
      render(<Dropdown content={content}>Button</Dropdown>);

      fireEvent.click(screen.getByText('Button'));

      await sleep(100);

      fireEvent.click(screen.getByText('Button'));
    });

    it('hides content', async () => {
      await waitFor(() => {
        expect(document.body.querySelector('.Popup2_visible')).not.toBeInTheDocument();
      });
    });

    it('does not unmount popup node', async () => {
      await waitFor(() => {
        expect(document.body.querySelector('.Popup2')).toBeInTheDocument();
      });
    });
  });

  describe('after outside click', () => {
    describe('if was open', () => {
      const realAddEventListener = document.addEventListener;
      let listeners = {};
      beforeEach(async () => {
        listeners = {};
        document.addEventListener = jest.fn((event, cb) => {
          const fn = jest.fn(cb as EventListener);
          realAddEventListener(event, jest.fn(fn));
          listeners[event] = fn;
        });

        render(<Dropdown content={content}>Button</Dropdown>);

        fireEvent.click(screen.getByText('Button'));
      });

      it('hides content', async () => {
        fireEvent.mouseDown(document.body);

        // @ts-ignore
        expect(listeners.mousedown).toBeCalled();

        fireEvent.mouseUp(document.body);

        await waitFor(() => {
          expect(document.body.querySelector('.Popup2_visible')).not.toBeInTheDocument();
        });
      });
    });
  });
});
