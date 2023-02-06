import React from 'react';
import { render, screen, act, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { overlayTestId } from 'components/Overlay/Overlay.constants';
import { ActionsPanel } from './ActionsPanel';
import { actionsStub } from './Stub/actionsStub';

describe('ActionsPanel', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });
  it('render', async () => {
    act(() => {
      render(<ActionsPanel actions={actionsStub} />);
    });
    expect(screen.queryByTestId('ActionsPanel')).toBeInTheDocument();
  });
  describe('when actions is empty', () => {
    it('does not render', async () => {
      act(() => {
        render(<ActionsPanel />);
      });
      expect(screen.queryByTestId('ActionsPanel')).not.toBeInTheDocument();
    });
    it('should not throw error', async () => {
      expect(() => {
        render(<ActionsPanel />);
      }).not.toThrow();
    });
  });
  describe('when actions has DropDown', () => {
    it('renders DropDown buttons ', async () => {
      act(() => {
        render(<ActionsPanel actions={actionsStub} />);
      });
      expect(screen.queryByText('DropDown1')).toBeInTheDocument();
      expect(screen.queryByText('DropDown2')).toBeInTheDocument();
    });
    describe('when clicks on DropDown', () => {
      it('renders DropDown menu', async () => {
        act(() => {
          render(<ActionsPanel actions={actionsStub} />);
        });
        userEvent.click(screen.getByText('DropDown1'));
        expect(screen.queryByText('DropDown1 Child1')).toBeInTheDocument();
        expect(screen.queryByText('DropDown1 Child2')).toBeInTheDocument();
        expect(screen.queryByText('DropDown1 Child3')).toBeInTheDocument();
      });
    });
    describe('when clicks on DropDown menu item', () => {
      it('calls onItemClick', async () => {
        const handleItemClick = jest.fn();
        act(() => {
          render(<ActionsPanel onItemClick={handleItemClick} actions={actionsStub} />);
        });
        const action = actionsStub.find((action) => action.caption === 'DropDown1 Child1');
        userEvent.click(screen.getByText('DropDown1'));
        userEvent.click(screen.getByText('DropDown1 Child1'));
        expect(handleItemClick).toBeCalledTimes(1);
        expect(handleItemClick).toBeCalledWith(action);
      });
    });
  });
  describe('when actions has Button', () => {
    it('renders buttons ', async () => {
      act(() => {
        render(<ActionsPanel actions={actionsStub} />);
      });
      expect(screen.queryByText('ActionButton')).toBeInTheDocument();
    });
    describe('when clicks on Button', () => {
      it('calls onItemClick', async () => {
        const handleItemClick = jest.fn();
        act(() => {
          render(<ActionsPanel onItemClick={handleItemClick} actions={actionsStub} />);
        });
        const action = actionsStub.find((action) => action.caption === 'ActionButton');
        userEvent.click(screen.getByText('ActionButton'));
        expect(handleItemClick).toBeCalledTimes(1);
        expect(handleItemClick).toBeCalledWith(action);
      });
    });
  });
  describe('when is loading', () => {
    it('renders overlay', async () => {
      act(() => {
        render(<ActionsPanel actions={actionsStub} isLoading />);
      });
      expect(screen.queryByTestId(overlayTestId)).toBeInTheDocument();
    });
  });
});
