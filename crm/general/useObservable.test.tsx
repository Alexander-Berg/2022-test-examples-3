import { renderHook, act } from '@testing-library/react-hooks';
import { render } from '@testing-library/react';
import { Subject, ReplaySubject, timer } from 'rxjs';
import { finalize } from 'rxjs/operators';
import React, { useEffect, FC } from 'react';
import { useObservable } from './useObservable';

describe('useObservable', () => {
  it('has default value on init', () => {
    const subject = new Subject<boolean>();

    const { result } = renderHook(() => useObservable(subject));

    expect(result.current).toStrictEqual({ complete: false, error: undefined, next: undefined });
  });

  it('updates on next value', () => {
    const subject = new Subject<boolean>();

    const { result } = renderHook(() => useObservable(subject));

    act(() => {
      subject.next(true);
    });

    expect(result.current).toStrictEqual({ complete: false, error: undefined, next: true });
  });

  it('updates on error', () => {
    const subject = new Subject<boolean>();

    const { result } = renderHook(() => useObservable(subject));

    const error = new Error('error');

    act(() => {
      subject.error(error);
    });

    expect(result.current).toStrictEqual({ complete: false, error, next: undefined });
  });

  it('updates on complete', () => {
    const subject = new Subject<boolean>();

    const { result } = renderHook(() => useObservable(subject));

    act(() => {
      subject.complete();
    });

    expect(result.current).toStrictEqual({ complete: true, error: undefined, next: undefined });
  });

  describe('when change observable', () => {
    it('unsubscribes from old', () => {
      const finalizeSpy = jest.fn();
      const observable1 = timer(1000).pipe(finalize(finalizeSpy));
      const observable2 = timer(1000);

      const { rerender } = renderHook(({ observable }) => useObservable(observable), {
        initialProps: { observable: observable1 },
      });

      rerender({ observable: observable2 });

      expect(finalizeSpy).toBeCalled();
    });

    it('subscribes to new', () => {
      const subject1 = new Subject<boolean>();
      const subject2 = new Subject<boolean>();

      const { result, rerender } = renderHook(({ subject }) => useObservable(subject), {
        initialProps: { subject: subject1 },
      });
      act(() => {
        subject1.next(false);
        subject2.next(true);
      });

      rerender({ subject: subject2 });

      act(() => {
        subject2.next(true);
        subject1.next(false);
      });

      expect(result.current).toStrictEqual({ complete: false, error: undefined, next: true });
    });
  });

  describe('useEffect specificity', () => {
    interface Props {
      subject: Subject<boolean>;
    }

    const Child: FC<Props> = ({ subject }) => {
      useEffect(() => {
        subject.next(true);
      }, []);

      return null;
    };

    const nextLogger = jest.fn();

    const Parent: FC<Props> = ({ subject }) => {
      const { next } = useObservable(subject);

      nextLogger(next);

      return <Child subject={subject} />;
    };

    beforeEach(() => {
      nextLogger.mockClear();
    });

    describe('when use subject', () => {
      it('can skip next value', () => {
        const subject = new Subject<boolean>();

        render(<Parent subject={subject} />);

        expect(nextLogger).toBeCalledTimes(1);
        expect(nextLogger).toBeCalledWith(undefined);
      });
    });

    describe('when use replay subject', () => {
      it('catches next value', () => {
        const subject = new ReplaySubject<boolean>();

        render(<Parent subject={subject} />);

        expect(nextLogger).toBeCalledTimes(2);
        expect(nextLogger).toBeCalledWith(true);
      });
    });
  });
});
