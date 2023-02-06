import { computeGroup } from '../../Group.utils';

describe('computeGroup', () => {
  describe('when length is 1', () => {
    it('returns round-round', () => {
      expect(
        computeGroup({
          length: 1,
          index: 0,
        }),
      ).toBe('round-round');
    });
  });

  describe('when index is first', () => {
    it('returns round-brick', () => {
      expect(
        computeGroup({
          length: 2,
          index: 0,
        }),
      ).toBe('round-brick');
    });
  });

  describe('when index is somewhere in the middle', () => {
    it('returns clear-brick', () => {
      expect(
        computeGroup({
          length: 4,
          index: 2,
        }),
      ).toBe('clear-brick');
    });
  });

  describe('when index is last', () => {
    it('returns the clear-round', () => {
      expect(
        computeGroup({
          length: 6,
          index: 5,
        }),
      ).toBe('clear-round');
    });
  });

  describe('when isScrollable is true', () => {
    describe('when index is first', () => {
      it('returns the top-scroll', () => {
        expect(
          computeGroup({
            length: 8,
            index: 0,
            isScrollable: true,
          }),
        ).toBe('top-scroll');
      });
    });

    describe('when index is somewhere in the middle', () => {
      it('returns the clear-brick', () => {
        expect(
          computeGroup({
            length: 8,
            index: 3,
            isScrollable: true,
          }),
        ).toBe('clear-brick');
      });
    });

    describe('when index is last', () => {
      it('returns the bottom-scroll', () => {
        expect(
          computeGroup({
            length: 8,
            index: 7,
            isScrollable: true,
          }),
        ).toBe('bottom-scroll');
      });
    });
  });
});
