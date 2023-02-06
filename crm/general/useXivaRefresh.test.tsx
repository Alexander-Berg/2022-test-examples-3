import React, { FC } from 'react';
import { render, waitFor } from '@testing-library/react';
import { ArgumentsOf } from 'ts-jest/dist/utils/testing';
import { CRMXiva, XivaBackendEventType, XivaBackendEvents } from 'modules/xiva';
import { useXivaRefresh } from './useXivaRefresh';

const TestComponent: FC<ArgumentsOf<typeof useXivaRefresh>[0]> = (props) => {
  useXivaRefresh(props);

  return null;
};

jest.mock('services/Config', () => ({
  config: {
    value: {
      features: {
        useTicketStructuredFilters: true,
      },
    },
  },
}));

describe('useXivaRefresh', () => {
  let storage;
  let xiva: CRMXiva;
  let defaultAccessors;

  beforeEach(() => {
    storage = {
      1: {
        counter: 1,
      },
      2: {
        counter: 2,
      },
      3: {
        counter: 3,
      },
    };

    xiva = ({
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
    } as unknown) as CRMXiva;

    defaultAccessors = {
      xiva,
      has: (id) => Boolean(storage[id]),
      getById: (id) => storage[id],
      update: (update) => {
        storage = { ...storage, ...update };
      },
    };
  });

  describe('on mount', () => {
    it('subscribes on CountersChanged', () => {
      render(<TestComponent {...defaultAccessors} />);

      expect(defaultAccessors.xiva.addEventListener).toBeCalled();
      expect(defaultAccessors.xiva.addEventListener).toBeCalledWith(
        XivaBackendEventType.CountersChanged,
        expect.any(Function),
      );
    });
  });

  describe('on unmount', () => {
    it('unsubscribes from CountersChanged', () => {
      const { unmount } = render(<TestComponent {...defaultAccessors} />);

      unmount();

      expect(defaultAccessors.xiva.removeEventListener).toBeCalled();
      expect(defaultAccessors.xiva.removeEventListener).toBeCalledWith(
        XivaBackendEventType.CountersChanged,
        expect.any(Function),
      );
    });
  });

  describe('on CountersChanged', () => {
    it('updates storage', async () => {
      let handler: Function;
      xiva.addEventListener = jest.fn((event, eventHandler) => {
        handler = eventHandler as Function;
      });
      render(<TestComponent {...defaultAccessors} />);

      await waitFor(() => {
        expect(xiva.addEventListener).toBeCalled();
      });

      const event = {
        detail: [
          {
            id: 1,
            value: '228',
          },
          {
            id: 2,
            value: '1337',
          },
          {
            id: 4,
            value: '4',
          },
        ],
      } as XivaBackendEvents[XivaBackendEventType.CountersChanged];
      handler!(event);

      expect(storage).toEqual({
        1: {
          counter: 228,
        },
        2: {
          counter: 1337,
        },
        3: {
          counter: 3,
        },
      });
    });
  });
});
