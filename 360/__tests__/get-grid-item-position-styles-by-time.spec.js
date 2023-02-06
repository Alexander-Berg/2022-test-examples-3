import GridItemPosition from '../GridItemPosition';
import getGridItemPostionStylesByTime from '../get-grid-item-position-styles-by-time';

describe('get-grid-item-position-styles-by-time', () => {
  it('должен вычислять стили позиционирования', () => {
    const itemPos = new GridItemPosition({
      column: 1,
      row: 705,
      spanColumns: 1,
      spanRows: 60,
      columnsTotal: 2,
      groupDepth: 1,
      groupHeight: 1
    });

    const opts = {
      minHeight: 13,
      itemsGap: 1,
      rowHeight: 0.9333333333333333,
      hourSepHeight: 1,
      groupShift: 5,
      marginRight: 10
    };

    const positionStyles = getGridItemPostionStylesByTime(itemPos, opts);

    expect(positionStyles).toEqual({
      top: '659px',
      height: '53px',
      width: 'calc(((100% - 20px) / 2) * 1 - 1px)',
      left: 'calc(1 * ((100% - 20px) / 2) + 6px)'
    });
  });
});
