import mobileAppManager from 'utils/applicationInterface';
import * as environment from 'configs/environment';

import {addListeners} from '../detectNetwork';

describe('detectNetwork', () => {
  describe('addListeners', () => {
    beforeEach(() => {
      jest
        .spyOn(mobileAppManager, 'registerChangeOnlineStatusHandler')
        .mockImplementationOnce(() => {});
      jest.spyOn(window, 'addEventListener').mockImplementationOnce(() => {});
    });

    test('если это мобильный оффлайн календарь, должен навесить обработчики при смене сети в mobileAppManager', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(true);
      addListeners(jest.fn());
      expect(mobileAppManager.registerChangeOnlineStatusHandler).toHaveBeenCalledWith(
        expect.any(Function)
      );
    });

    test('если это не мобильный оффлайн календарь, должен навесить обработчики при смене сети на window', () => {
      addListeners(jest.fn());

      expect(window.addEventListener).toHaveBeenCalledTimes(2);
      expect(window.addEventListener).toHaveBeenCalledWith('online', expect.any(Function));
      expect(window.addEventListener).toHaveBeenCalledWith('offline', expect.any(Function));
    });
  });
});
