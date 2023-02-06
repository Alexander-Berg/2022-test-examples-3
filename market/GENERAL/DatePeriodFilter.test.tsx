import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

import * as dateUtils from './utils';
import { DatePeriodFilterProps as ComponentProps, DatePeriodFilter } from './DatePeriodFilter';

describe('<DatePeriodFilter />', () => {
  const DEFAULT_PROPS: ComponentProps = {
    onChange: jest.fn(),
    isLoading: false,
    startDate: null,
    endDate: null,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderWithProps = (props: ComponentProps = DEFAULT_PROPS) => {
    return render(<DatePeriodFilter {...props} />);
  };
  const getStartDateInput = () => screen.getByPlaceholderText('От');
  const getEndDateInput = () => screen.getByPlaceholderText('До');
  const getButtonWithText = (buttonText: string) =>
    screen.getByRole('button', {
      name: buttonText,
    });
  const CURRENT_WEEK_BUTTON_TEXT = 'Текущая неделя';
  const CURRENT_MONTH_BUTTON_TEXT = 'Текущий месяц';
  const NEXT_MONTH_BUTTON_TEXT = 'Следующий месяц';

  const TEST_DATE = '2020-12-28';
  const TEST_DATE_INPUT_VALUE = '28.12.2020';

  const CURRENT_MONTH_NUMBER = new Date().getMonth();

  it('render without errors and contains inputs with empty values', () => {
    renderWithProps();

    const startDateInput = getStartDateInput();
    expect(startDateInput).toBeInTheDocument();
    expect(startDateInput).toHaveProperty('value', '');

    const endDateInput = getEndDateInput();
    expect(endDateInput).toBeInTheDocument();
    expect(endDateInput).toHaveProperty('value', '');

    expect(getButtonWithText(CURRENT_WEEK_BUTTON_TEXT)).toBeInTheDocument();
    expect(getButtonWithText(CURRENT_MONTH_BUTTON_TEXT)).toBeInTheDocument();
    expect(getButtonWithText(NEXT_MONTH_BUTTON_TEXT)).toBeInTheDocument();
  });

  it('render without errors and contains inputs with start date', () => {
    renderWithProps({
      ...DEFAULT_PROPS,
      startDate: TEST_DATE,
    });

    expect(getStartDateInput()).toHaveProperty('value', TEST_DATE_INPUT_VALUE);
  });

  it('render without errors and contains inputs with end date', () => {
    renderWithProps({
      ...DEFAULT_PROPS,
      startDate: TEST_DATE,
    });

    expect(getStartDateInput()).toHaveProperty('value', TEST_DATE_INPUT_VALUE);
  });

  it('entering a start date triggers a change callback', () => {
    renderWithProps();

    fireEvent.change(getStartDateInput(), { target: { value: TEST_DATE_INPUT_VALUE } });

    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledWith({ startDate: TEST_DATE });
  });

  it('click on current week button triggers a change callback', () => {
    jest.spyOn(dateUtils, 'getCurrentMondayDate').mockImplementation(() => TEST_DATE);
    jest.spyOn(dateUtils, 'getCurrentSundayDate').mockImplementation(() => TEST_DATE);
    renderWithProps();

    fireEvent.click(getButtonWithText(CURRENT_WEEK_BUTTON_TEXT));

    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledWith({
      startDate: TEST_DATE,
      endDate: TEST_DATE,
    });
  });

  it('click on current month button triggers a change callback', () => {
    const getMonthFirstDateMock = jest.spyOn(dateUtils, 'getMonthFirstDate').mockImplementation(() => TEST_DATE);
    const getMonthLastDateMock = jest.spyOn(dateUtils, 'getMonthLastDate').mockImplementation(() => TEST_DATE);
    renderWithProps();

    fireEvent.click(getButtonWithText(CURRENT_MONTH_BUTTON_TEXT));

    expect(getMonthFirstDateMock).toHaveBeenCalledWith(CURRENT_MONTH_NUMBER);
    expect(getMonthLastDateMock).toHaveBeenCalledWith(CURRENT_MONTH_NUMBER);

    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledWith({
      startDate: TEST_DATE,
      endDate: TEST_DATE,
    });
  });

  it('click on next month button triggers a change callback', () => {
    const getMonthFirstDateMock = jest.spyOn(dateUtils, 'getMonthFirstDate').mockImplementation(() => TEST_DATE);
    const getMonthLastDateMock = jest.spyOn(dateUtils, 'getMonthLastDate').mockImplementation(() => TEST_DATE);
    renderWithProps();

    fireEvent.click(getButtonWithText(NEXT_MONTH_BUTTON_TEXT));

    expect(getMonthFirstDateMock).toHaveBeenCalledWith(CURRENT_MONTH_NUMBER + 1);
    expect(getMonthLastDateMock).toHaveBeenCalledWith(CURRENT_MONTH_NUMBER + 1);

    expect(DEFAULT_PROPS.onChange).toHaveBeenCalledWith({
      startDate: TEST_DATE,
      endDate: TEST_DATE,
    });
  });
});
