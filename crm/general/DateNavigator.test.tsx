import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { DateNavigator } from '.';

describe('DateNavigator', () => {
  describe('today button', () => {
    it("disabled when today's date is selected", () => {
      render(<DateNavigator value={new Date()} />);

      const todayButton = screen.getByRole('button', { name: 'Сегодня' });
      expect(todayButton).toBeDisabled();
    });

    it('enabled when today is not selected', () => {
      const date = new Date();
      date.setDate(date.getDate() + 1);
      render(<DateNavigator value={date} />);

      const todayButton = screen.getByRole('button', { name: 'Сегодня' });
      expect(todayButton).not.toBeDisabled();
    });
  });

  describe('prev button', () => {
    it('changes date to previous day', () => {
      const date = new Date();
      const prevDate = new Date(date);
      prevDate.setDate(prevDate.getDate() - 1);

      const fn = jest.fn();

      render(<DateNavigator value={date} onChange={fn} />);

      const prevButton = screen.getByTestId('prev-date-button');
      fireEvent.click(prevButton);

      expect(fn).toBeCalledWith(prevDate);
    });
  });

  describe('next button', () => {
    it('changes date to next day', () => {
      const date = new Date();
      const nextDate = new Date(date);
      nextDate.setDate(nextDate.getDate() + 1);

      const fn = jest.fn();

      render(<DateNavigator value={date} onChange={fn} />);

      const nextButton = screen.getByTestId('next-date-button');
      fireEvent.click(nextButton);

      expect(fn).toBeCalledWith(nextDate);
    });
  });

  describe('selected date button', () => {
    it('opens date picker', () => {
      render(<DateNavigator value={new Date()} />);

      const dateButton = screen.getByTestId('selected-date-button');
      fireEvent.click(dateButton);

      const datePicker = screen.getByTestId('Popup');
      expect(datePicker).toBeVisible();
    });
  });
});
