import { renderHighlightedText } from '../Item.utils';
import { Range } from '../../../../../../../types';

describe('Item.utils', () => {
  describe('renderHighlightedText', () => {
    const ignoreHighlighting = () => null;
    describe('when range in the start', () => {
      const highlightRanges: Range[] = [[0, 2]];
      it('renders highlighting', () => {
        expect(renderHighlightedText('test text', highlightRanges, ignoreHighlighting)).toEqual([
          null,
          't text',
        ]);
      });
    });

    describe('when range in the middle', () => {
      const highlightRanges: Range[] = [[3, 6]];
      it('renders highlighting', () => {
        expect(renderHighlightedText('test text', highlightRanges, ignoreHighlighting)).toEqual([
          'tes',
          null,
          'xt',
        ]);
      });
    });

    describe('when range in the end', () => {
      const highlightRanges: Range[] = [[5, 8]];
      it('renders highlighting', () => {
        expect(renderHighlightedText('test text', highlightRanges, ignoreHighlighting)).toEqual([
          'test ',
          null,
        ]);
      });
    });

    describe('multiple ranges', () => {
      const highlightRanges: Range[] = [
        [0, 2],
        [4, 6],
        [8, 8],
      ];
      it('renders every highlighting', () => {
        expect(renderHighlightedText('test text', highlightRanges, ignoreHighlighting)).toEqual([
          null,
          't',
          null,
          'x',
          null,
        ]);
      });
    });
  });
});
