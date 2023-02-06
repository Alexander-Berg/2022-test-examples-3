import { getBoundingClientRect } from './getBoundingClientRect';
import { PopupClientRect, PopupRect } from '../types';

describe('libs/popup', () => {
  describe('dom-utils/getBoundingClientRect', () => {
    it('should call getBoundingClientRect method', () => {
      const rect: PopupRect = {
        width: 200,
        height: 100,
        x: 50,
        y: 50,
      };
      const clientRect: PopupClientRect = {
        ...rect,
        top: rect.y,
        right: rect.x + rect.width,
        bottom: rect.y + rect.height,
        left: rect.x,
      };
      const getBoundingClientRectMock = jest.fn(() => clientRect);
      const mockEl = ({
        getBoundingClientRect: getBoundingClientRectMock,
      } as any) as Element;

      const result = getBoundingClientRect(mockEl);
      expect(getBoundingClientRectMock).toBeCalled();
      expect(result).toEqual(clientRect);
    });
  });
});
