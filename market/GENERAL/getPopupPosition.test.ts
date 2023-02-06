import { getPopupPosition } from './getPopupPosition';
import { rectToClientRect } from './rectToClientRect';
import { PopupDirection } from '../types';

describe('libs/popup', () => {
  describe('utils/getPopupPosition', () => {
    describe('directions', () => {
      const anchorDimensions = rectToClientRect({
        height: 100,
        width: 100,
        x: 200,
        y: 200,
      });

      const popupDimensions = rectToClientRect({
        height: 50,
        width: 50,
        x: 0,
        y: 0,
      });

      const viewportDimensions = rectToClientRect({
        height: 500,
        width: 500,
        x: 0,
        y: 0,
      });

      it.each<[PopupDirection, { top: number; left: number }]>([
        ['bottom-left', { top: 300, left: 200 }],
        ['bottom-center', { top: 300, left: 225 }],
        ['bottom-right', { top: 300, left: 250 }],
        ['top-left', { top: 150, left: 200 }],
        ['top-center', { top: 150, left: 225 }],
        ['top-right', { top: 150, left: 250 }],
        ['left-top', { top: 200, left: 150 }],
        ['left-center', { top: 225, left: 150 }],
        ['left-bottom', { top: 250, left: 150 }],
        ['right-top', { top: 200, left: 300 }],
        ['right-center', { top: 225, left: 300 }],
        ['right-bottom', { top: 250, left: 300 }],
      ])('should calculate position for %s direction', (direction, expected) => {
        const { top, left } = getPopupPosition({
          direction,
          anchorDimensions,
          popupDimensions,
          viewportDimensions,
        });

        expect(top).toBe(expected.top);
        expect(left).toBe(expected.left);
      });
    });
  });
});
