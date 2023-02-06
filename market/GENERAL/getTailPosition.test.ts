import { getTailPosition } from './getTailPosition';
import { rectToClientRect } from './rectToClientRect';
import { PopupDirection } from '../types';

describe('libs/popup', () => {
  describe('utils/getTailPosition', () => {
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

      it.each<[PopupDirection, { top: number; left: number }]>([
        ['bottom-left', { top: -6, left: 19 }],
        ['bottom-center', { top: -6, left: 19 }],
        ['bottom-right', { top: -6, left: 19 }],
        ['top-left', { top: 44, left: 19 }],
        ['top-center', { top: 44, left: 19 }],
        ['top-right', { top: 44, left: 19 }],
        ['left-top', { top: 19, left: 44 }],
        ['left-center', { top: 19, left: 44 }],
        ['left-bottom', { top: 19, left: 44 }],
        ['right-top', { top: 19, left: -6 }],
        ['right-center', { top: 19, left: -6 }],
        ['right-bottom', { top: 19, left: -6 }],
      ])('should calculate position for %s direction', (direction, expected) => {
        const { top, left } = getTailPosition({
          tailSize: 12,
          direction,
          anchorDimensions,
          popupDimensions,
        });

        expect(top).toBe(expected.top);
        expect(left).toBe(expected.left);
      });
    });
  });
});
