import { handleUnsuccessful } from '../handleUnsuccessful';
import { BackendError } from '../BackendError';

describe('handleUnsuccessful', () => {
  it('should convert to backend error', () => {
    return Promise.resolve({
      status: 200,
      message: 'message',
      success: false,
    })
      .then(handleUnsuccessful)
      .catch((error) => expect(error).toBeInstanceOf(BackendError));
  });

  it('should not convert to backend error', () => {
    return Promise.resolve({
      status: 200,
      message: 'message',
      success: true,
    })
      .then(handleUnsuccessful)
      .catch((error) => expect(error).toBe('no error here'));
  });
});
