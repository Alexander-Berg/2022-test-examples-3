import React, { createRef, useRef, useEffect } from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { FloatingDateInstance } from 'components/FloatingDate';
import { FloatingDate } from './FloatingDate';

describe('FloatingDate', () => {
  it('exposes instance with "handleScroll" method', () => {
    const floatingDateRef = createRef<FloatingDateInstance>();
    render(
      <TestBed>
        <FloatingDate ref={floatingDateRef} />
      </TestBed>,
    );

    expect(floatingDateRef.current!.handleScroll).toEqual(expect.any(Function));
  });

  it('renders date', async () => {
    const formatDate = (date: string) => {
      return new Date(date).getDate().toString();
    };
    const TestComponent = () => {
      const floatingDateRef = useRef<FloatingDateInstance>(null);

      useEffect(() => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 26).toISOString(),
          firstVisibleHeight: 100,
          firstHiddenHeight: 300,
          bottomOffset: 0,
        });
      }, []);

      return <FloatingDate formatDate={formatDate} ref={floatingDateRef} />;
    };

    render(
      <TestBed>
        <TestComponent />
      </TestBed>,
    );

    await waitFor(() => {
      expect(screen.getByText('26')).toBeInTheDocument();
    });
  });

  it('renders previous date', async () => {
    const formatDate = (date: string) => {
      return new Date(date).getDate().toString();
    };
    const TestComponent = () => {
      const floatingDateRef = useRef<FloatingDateInstance>(null);

      useEffect(() => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 26).toISOString(),
          prevVisibleDate: new Date(2021, 9, 25).toISOString(),
          firstVisibleHeight: 100,
          firstHiddenHeight: 300,
          bottomOffset: 0,
        });
      }, []);

      const scrollToPreviousDate = () => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 25).toISOString(),
          nextVisibleDate: new Date(2021, 9, 26).toISOString(),
          firstVisibleHeight: 50,
          firstHiddenHeight: 200,
          bottomOffset: 350,
        });
      };

      return (
        <>
          <button onClick={scrollToPreviousDate}>scroll to previous date</button>
          <FloatingDate formatDate={formatDate} ref={floatingDateRef} />
        </>
      );
    };

    render(
      <TestBed>
        <TestComponent />
      </TestBed>,
    );

    userEvent.click(screen.getByRole('button'));

    await waitFor(() => {
      expect(screen.getByText('25')).toBeInTheDocument();
    });
  });

  it('renders next date', async () => {
    const formatDate = (date: string) => {
      return new Date(date).getDate().toString();
    };
    const TestComponent = () => {
      const floatingDateRef = useRef<FloatingDateInstance>(null);

      useEffect(() => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 26).toISOString(),
          nextVisibleDate: new Date(2021, 9, 27).toISOString(),
          firstVisibleHeight: 100,
          firstHiddenHeight: 300,
          bottomOffset: 400,
        });
      }, []);

      const scrollToNextDate = () => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 27).toISOString(),
          prevVisibleDate: new Date(2021, 9, 26).toISOString(),
          firstVisibleHeight: 200,
          firstHiddenHeight: 200,
          bottomOffset: 0,
        });
      };

      return (
        <>
          <button onClick={scrollToNextDate}>scroll to next date</button>
          <FloatingDate formatDate={formatDate} ref={floatingDateRef} />
        </>
      );
    };

    render(
      <TestBed>
        <TestComponent />
      </TestBed>,
    );

    userEvent.click(screen.getByRole('button'));

    await waitFor(() => {
      expect(screen.getByText('27')).toBeInTheDocument();
    });
  });

  it('renders both dates smoothly', async () => {
    const formatDate = (date: string) => {
      return new Date(date).getDate().toString();
    };
    const TestComponent = () => {
      const floatingDateRef = useRef<FloatingDateInstance>(null);

      useEffect(() => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 26).toISOString(),
          nextVisibleDate: new Date(2021, 9, 27).toISOString(),
          firstVisibleHeight: 10,
          firstHiddenHeight: 150,
          bottomOffset: 100,
        });
      }, []);

      const scrollToPreviousDate = () => {
        floatingDateRef.current!.handleScroll({
          firstVisibleDate: new Date(2021, 9, 26).toISOString(),
          nextVisibleDate: new Date(2021, 9, 27).toISOString(),
          firstVisibleHeight: 14,
          firstHiddenHeight: 196,
          bottomOffset: 104,
        });
      };

      return (
        <>
          <button onClick={scrollToPreviousDate}>scroll to previous date</button>
          <FloatingDate formatDate={formatDate} ref={floatingDateRef} />
        </>
      );
    };

    render(
      <TestBed>
        <TestComponent />
      </TestBed>,
    );

    userEvent.click(screen.getByRole('button'));

    await waitFor(() => {
      expect(screen.getByText('26')).toBeInTheDocument();
      expect(screen.getByText('27')).toBeInTheDocument();
    });
  });
});
