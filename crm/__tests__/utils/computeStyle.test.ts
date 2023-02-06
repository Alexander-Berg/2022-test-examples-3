import { computeStyle } from '../../Group.utils';
import { LISTITEM_BORDER_RADIUS } from '../../Group.constants';

describe('computeStyle', () => {
  it('it subtracts the ListItem border-radius from scrollbarWidth', () => {
    expect(
      computeStyle({
        scrollbarWidth: 70,
        isScrollable: true,
      }),
    ).toEqual({
      paddingRight: `${70 - LISTITEM_BORDER_RADIUS}px`,
    });
  });

  describe('when isScrollable is true', () => {
    it('returns style with right padding', () => {
      expect(
        computeStyle({
          scrollbarWidth: 20,
          isScrollable: true,
        }),
      ).toEqual({
        paddingRight: '12px',
      });
    });
  });

  describe('when isScrollable is false', () => {
    it('returns empty style', () => {
      expect(
        computeStyle({
          scrollbarWidth: 20,
          isScrollable: false,
        }),
      ).toEqual({});
    });
  });
});
