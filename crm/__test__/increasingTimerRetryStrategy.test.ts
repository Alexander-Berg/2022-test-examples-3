import { throwError } from 'rxjs';
import { catchError, retryWhen } from 'rxjs/operators';
import {
  increasingTimerRetryStrategy,
  DEFAULT_MAX_RETRY_ATTEMPTS,
  DEFAULT_SCALING_DURATION,
  IncreasingTimerRetryStrategyOptions,
} from '../increasingTimerRetryStrategy';

jest.useFakeTimers('modern');

describe('increasingTimerRetryStrategy', () => {
  const createTestSetup = (options?: IncreasingTimerRetryStrategyOptions) => {
    const mockTap = jest.fn();
    throwError('error')
      .pipe(
        catchError((error) => {
          mockTap();
          return throwError(error);
        }),
        retryWhen(increasingTimerRetryStrategy(options)),
      )
      .subscribe({ error: () => {} });

    return mockTap;
  };

  it('uses default options', () => {
    const mockTap = createTestSetup();

    jest.advanceTimersByTime(
      DEFAULT_MAX_RETRY_ATTEMPTS * DEFAULT_MAX_RETRY_ATTEMPTS * DEFAULT_SCALING_DURATION,
    );

    expect(mockTap).toBeCalledTimes(DEFAULT_MAX_RETRY_ATTEMPTS + 1);
  });

  it('uses custom options', () => {
    const scalingDuration = 10000;
    const maxRetryAttempts = 2;
    const mockTap = createTestSetup({
      maxRetryAttempts,
      scalingDuration,
    });

    jest.advanceTimersByTime(maxRetryAttempts * maxRetryAttempts * scalingDuration);

    expect(mockTap).toBeCalledTimes(maxRetryAttempts + 1);
  });

  it('increases retry timer', () => {
    const scalingDuration = 10000;
    const deltaDuration = scalingDuration / 10;
    const maxRetryAttempts = 2;
    const mockTap = createTestSetup({
      maxRetryAttempts,
      scalingDuration,
    });

    expect(mockTap).toBeCalledTimes(1);
    jest.advanceTimersByTime(scalingDuration - deltaDuration);
    expect(mockTap).toBeCalledTimes(1);
    jest.advanceTimersByTime(2 * deltaDuration);
    expect(mockTap).toBeCalledTimes(2);
    jest.advanceTimersByTime(2 * scalingDuration - 2 * deltaDuration);
    expect(mockTap).toBeCalledTimes(2);
    jest.advanceTimersByTime(2 * deltaDuration);
    expect(mockTap).toBeCalledTimes(3);
  });
});
