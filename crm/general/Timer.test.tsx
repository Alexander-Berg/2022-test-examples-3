import { MINUTES_IN_HOUR, MS_IN_SECOND, SECONDS_IN_MINUTE } from 'constants/time';
import React from 'react';
import { act, render, screen } from '@testing-library/react';
import { Timer } from './Timer';
import { TestBed } from '../TestBed';

const startDate = new Date(2022, 1, 1, 1, 10);
const endDate = new Date(2022, 1, 1, 1, 12);

describe('Timer', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders timer (1)', () => {
    render(
      <TestBed>
        <Timer startDate={`${startDate}`} endDate={`${endDate}`} isActive />
      </TestBed>,
    );

    expect(screen.getByText('02:00')).toBeVisible();

    act(() => {
      jest.advanceTimersByTime(MS_IN_SECOND);
    });

    expect(screen.getByText('02:01')).toBeVisible();

    act(() => {
      jest.advanceTimersByTime(MS_IN_SECOND * SECONDS_IN_MINUTE);
    });

    expect(screen.getByText('03:01')).toBeVisible();

    act(() => {
      jest.advanceTimersByTime(MS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR);
    });

    expect(screen.getByText('01:03:01')).toBeVisible();
  });

  it('renders timer (2)', () => {
    render(
      <TestBed>
        <Timer timePassedMs={2000} isActive />
      </TestBed>,
    );

    expect(screen.getByText('00:02')).toBeVisible();

    act(() => {
      jest.advanceTimersByTime(MS_IN_SECOND);
    });

    expect(screen.getByText('00:03')).toBeVisible();
  });

  it('correctly works when inactive (1)', () => {
    render(
      <TestBed>
        <Timer startDate={`${startDate}`} endDate={`${endDate}`} isActive={false} />
      </TestBed>,
    );

    expect(screen.getByText('02:00')).toBeVisible();

    act(() => {
      jest.advanceTimersByTime(MS_IN_SECOND);
    });

    expect(screen.getByText('02:00')).toBeVisible();
  });

  it('correctly works when inactive (2)', () => {
    render(
      <TestBed>
        <Timer timePassedMs={2000} isActive={false} />
      </TestBed>,
    );

    expect(screen.getByText('00:02')).toBeVisible();

    act(() => {
      jest.advanceTimersByTime(MS_IN_SECOND);
    });

    expect(screen.getByText('00:02')).toBeVisible();
  });
});
