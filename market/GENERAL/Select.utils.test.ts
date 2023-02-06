import { filterOptionsByLabel } from './Select.utils';

describe('<Select.utils />', () => {
  describe('filterOptionsByLabel', () => {
    it('filters', () => {
      expect(
        filterOptionsByLabel(
          [
            { label: '1', value: 1 },
            { label: '2', value: 2 },
            { label: '22', value: 22 },
            { label: '3', value: 3 },
          ],
          '2'
        )
      ).toEqual([
        { label: '2', value: 2 },
        { label: '22', value: 22 },
      ]);
    });

    it('cut lengs', () => {
      expect(
        filterOptionsByLabel(
          [
            { label: '1', value: 1 },
            { label: '2', value: 2 },
            { label: '22', value: 22 },
            { label: '3', value: 3 },
          ],
          '',
          2
        )
      ).toEqual([
        { label: '1', value: 1 },
        { label: '2', value: 2 },
      ]);
    });
  });
});
