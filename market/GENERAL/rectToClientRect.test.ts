import { PopupRect } from '../types';
import { rectToClientRect } from './rectToClientRect';

describe('libs/popup', () => {
  describe('utils/rectToClientRect', () => {
    it('should returns correct result', () => {
      const rect: PopupRect = {
        height: 10,
        width: 10,
        x: 6,
        y: 12,
      };

      expect(rectToClientRect(rect)).toEqual({
        ...rect,
        left: 6,
        top: 12,
        right: 16,
        bottom: 22,
      });
    });
  });
});
