/* eslint-disable @typescript-eslint/no-explicit-any */

import { processBackendError } from '../processBackendError';
import { BackendError } from '../BackendError';

describe('processBackendError', () => {
  it('should convert to backend error', () => {
    expect(
      processBackendError({
        response: {},
        status: 500,
        message: 'error',
      } as any),
    ).toBeInstanceOf(BackendError);
  });

  describe('when error has code 401 and redirectTo', () => {
    it('redirects to passport', () => {
      const location = jest.fn();

      Object.defineProperty(window, 'location', {
        set: location,
      });

      processBackendError({
        response: {},
        responseJSON: {
          redirectTo: 'https://redirect.to',
        },
        status: 401,
        message: 'error',
      } as any);
      expect(location).toBeCalledWith('https://redirect.to');
    });
  });
});
