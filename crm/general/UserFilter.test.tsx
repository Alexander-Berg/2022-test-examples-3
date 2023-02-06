import React from 'react';
import { render, screen, waitFor, cleanup, fireEvent } from '@testing-library/react/pure';
import { SortObject } from 'types/api/form/Form';
import { UserFilter } from './UserFilter';
import { TableController } from '../../../services/TableController';
import { UNKNOWN_FILTER_TYPE_MESSAGE } from './UserFilter.constants';
jest.mock('../../../services/TableController');

const testColumn = {
  id: 'col1',
  title: 'colTitle',
  type: 'Text',
  access: 3,
  sortable: true,
  isFieldsUpdateNeeded: false,
};

const testFilter = {
  type: 'MatchNumber',
};

const testSort = { id: 'col1', order: 'Asc' } as SortObject;

const controller = new TableController();
controller.addUserFilter = jest.fn((_columnId, _filter) => controller);
controller.addSort = jest.fn((_sort) => controller);
const closePopup = jest.fn();

const getApplyButton = () => screen.getByTestId('applyButton');

describe('UserFilter', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });
  describe('when filter type unknown', async () => {
    it('shows message', async () => {
      const filter = { ...testFilter, type: 'someType' };
      render(
        <UserFilter
          sort={testSort}
          column={testColumn}
          filter={filter}
          controller={controller}
          closePopup={closePopup}
        />,
      );
      await waitFor(() => {
        expect(screen.getByText(UNKNOWN_FILTER_TYPE_MESSAGE)).toBeInTheDocument();
      });
    });
  });

  describe('when filter type known', async () => {
    it('renders', async () => {
      render(
        <UserFilter
          sort={testSort}
          column={testColumn}
          filter={testFilter}
          controller={controller}
          closePopup={closePopup}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId('TableUserFilter')).toBeInTheDocument();
      });
    });

    describe('if nothing changed', async () => {
      it('holds button disabled', async () => {
        render(
          <UserFilter
            sort={testSort}
            column={testColumn}
            filter={testFilter}
            controller={controller}
            closePopup={closePopup}
          />,
        );
        await waitFor(() => {
          expect(getApplyButton()).toBeDisabled();
        });
      });
    });
    describe('if something changed', async () => {
      it('sets button enabled', async () => {
        render(
          <UserFilter
            sort={testSort}
            column={testColumn}
            filter={testFilter}
            controller={controller}
            closePopup={closePopup}
          />,
        );
        fireEvent.click(screen.getByText('По убыванию (Я – А)'));
        await waitFor(() => {
          expect(getApplyButton()).toBeEnabled();
        });
      });
    });
    describe('when apply changes sort', async () => {
      beforeEach(async () => {
        render(
          <UserFilter
            sort={testSort}
            column={testColumn}
            filter={testFilter}
            controller={controller}
            closePopup={closePopup}
          />,
        );
        await waitFor(() => fireEvent.click(screen.getByText('По убыванию (Я – А)')));
        await waitFor(() => fireEvent.click(getApplyButton()));
      });
      it('sets correct sort', async () => {
        expect(controller.addSort).toBeCalledTimes(1);
        expect(controller.addSort).toBeCalledWith({ id: 'col1', order: 'Desc' });
      });
    });
  });

  describe('when filter type "MatchNumber"', async () => {
    it('renders number input', async () => {
      render(
        <UserFilter
          sort={testSort}
          column={testColumn}
          filter={testFilter}
          controller={controller}
          closePopup={closePopup}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId('MatchNumber_Filter')).toBeInTheDocument();
      });
      expect(screen.getByTestId('MatchNumber_Filter')).toHaveAttribute('type', 'number');
    });
    describe('if apply changes', async () => {
      beforeEach(async () => {
        render(
          <UserFilter
            sort={testSort}
            column={testColumn}
            filter={testFilter}
            controller={controller}
            closePopup={closePopup}
          />,
        );
        await waitFor(() =>
          fireEvent.change(screen.getByTestId('MatchNumber_Filter'), { target: { value: 1001 } }),
        );
        await waitFor(() => fireEvent.click(getApplyButton()));
      });
      it('calls apply action with correct filter', async () => {
        expect(controller.addUserFilter).toBeCalledTimes(1);
        expect(controller.addUserFilter).toBeCalledWith('col1', {
          type: 'MatchNumber',
          data: { value: 1001 },
        });
        expect(controller.fetch).toBeCalledTimes(1);
        expect(closePopup).toBeCalledTimes(1);
      });
    });
  });

  describe('when filter type "RangeNumber"', () => {
    const testFilter = {
      type: 'RangeNumber',
    };
    it('renders fields "from" and "to"', async () => {
      render(
        <UserFilter
          sort={testSort}
          column={testColumn}
          filter={testFilter}
          controller={controller}
          closePopup={closePopup}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId('RangeNumber_Filter_From')).toBeInTheDocument();
      });
      await waitFor(() => {
        expect(screen.getByTestId('RangeNumber_Filter_To')).toBeInTheDocument();
      });
    });

    describe('if apply changes', () => {
      beforeEach(async () => {
        render(
          <UserFilter
            sort={testSort}
            column={testColumn}
            filter={testFilter}
            controller={controller}
            closePopup={closePopup}
          />,
        );
        await waitFor(() =>
          fireEvent.change(screen.getByTestId('RangeNumber_Filter_From'), {
            target: { value: 1000 },
          }),
        );
        await waitFor(() =>
          fireEvent.change(screen.getByTestId('RangeNumber_Filter_To'), {
            target: { value: 10000 },
          }),
        );
        await waitFor(() => fireEvent.click(getApplyButton()));
      });
      it('calls apply action with correct filter', async () => {
        expect(controller.addUserFilter).toBeCalledTimes(1);
        expect(controller.addUserFilter).toBeCalledWith('col1', {
          type: 'RangeNumber',
          data: { from: 1000, to: 10000 },
        });
        expect(controller.fetch).toBeCalledTimes(1);
        expect(closePopup).toBeCalledTimes(1);
      });
    });
  });

  describe('when filter type "ContainsString"', () => {
    const testFilter = {
      type: 'ContainsString',
    };
    it('renders text input', async () => {
      render(
        <UserFilter
          sort={testSort}
          column={testColumn}
          filter={testFilter}
          controller={controller}
          closePopup={closePopup}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId('ContainsString_Filter')).toBeInTheDocument();
      });
    });
    describe('if apply changes', async () => {
      beforeEach(async () => {
        render(
          <UserFilter
            sort={testSort}
            column={testColumn}
            filter={testFilter}
            controller={controller}
            closePopup={closePopup}
          />,
        );
        await waitFor(() =>
          fireEvent.change(screen.getByTestId('ContainsString_Filter'), {
            target: { value: 'test string' },
          }),
        );
        await waitFor(() => fireEvent.click(getApplyButton()));
      });
      it('calls apply action with correct filter', async () => {
        expect(controller.addUserFilter).toBeCalledTimes(1);
        expect(controller.addUserFilter).toBeCalledWith('col1', {
          type: 'ContainsString',
          data: { value: 'test string' },
        });
        expect(controller.fetch).toBeCalledTimes(1);
        expect(closePopup).toBeCalledTimes(1);
      });
    });
  });
});
