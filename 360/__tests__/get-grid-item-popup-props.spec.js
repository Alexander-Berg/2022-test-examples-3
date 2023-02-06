import moment from 'moment';

import EventRecord from 'features/events/EventRecord';

import TodoGroup from '../TodoGroup';
import getGridItemPopupProps, {POPUP_DIRECTIONS} from '../get-grid-item-popup-props';

describe('get-grid-item-popup-props', () => {
  describe(`вычисление позиции popup'а`, () => {
    it('должен целиться в позицию указателя на дневной сетке', () => {
      const gridItem = new EventRecord({});
      const gridView = 'day';
      const pointerPos = {top: 100, left: 100};
      const anchor = document.createElement('div');

      const props = getGridItemPopupProps({gridItem, gridView, pointerPos, anchor});

      expect(props.target).toEqual('position');
      expect(props.position).toEqual(pointerPos);
      expect(props.anchor).toBeUndefined();
    });

    it('должен целиться в HTML элемент на недельной сетке', () => {
      const gridItem = new EventRecord({});
      const gridView = 'week';
      const pointerPos = {top: 100, left: 100};
      const anchor = document.createElement('div');

      const props = getGridItemPopupProps({gridItem, gridView, pointerPos, anchor});

      expect(props.target).toBeUndefined();
      expect(props.position).toBeUndefined();
      expect(props.anchor).toEqual(anchor);
    });

    it('должен целиться в HTML элемент на месячной сетке', () => {
      const gridItem = new EventRecord({});
      const gridView = 'month';
      const pointerPos = {top: 100, left: 100};
      const anchor = document.createElement('div');

      const props = getGridItemPopupProps({gridItem, gridView, pointerPos, anchor});

      expect(props.target).toBeUndefined();
      expect(props.position).toBeUndefined();
      expect(props.anchor).toEqual(anchor);
    });
  });

  describe(`вычисление доступных направлений popup'а для событий`, () => {
    it('должен возвращать default направления для common событий на месячной сетке', () => {
      const gridItem = new EventRecord({
        start: Number(moment('2000-01-01T00:00:00')),
        end: Number(moment('2000-01-01T01:00:00'))
      });
      const gridView = 'month';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.default);
    });

    it('должен возвращать default направления для common событий на недельной сетке', () => {
      const gridItem = new EventRecord({
        start: Number(moment('2000-01-01T00:00:00')),
        end: Number(moment('2000-01-01T01:00:00'))
      });
      const gridView = 'week';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.default);
    });

    it('должен возвращать default направления для common событий на дневной сетке', () => {
      const gridItem = new EventRecord({
        start: Number(moment('2000-01-01T00:00:00')),
        end: Number(moment('2000-01-01T01:00:00'))
      });
      const gridView = 'day';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.default);
    });

    it('должен возвращать default направления для allday событий на месячной сетке', () => {
      const gridItem = new EventRecord({
        start: Number(moment('2000-01-01')),
        end: Number(moment('2000-01-02'))
      });
      const gridView = 'month';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.default);
    });

    it('должен возвращать allday направления для allday событий на недельной сетке', () => {
      const gridItem = new EventRecord({
        start: Number(moment('2000-01-01')),
        end: Number(moment('2000-01-02'))
      });
      const gridView = 'week';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.allday);
    });

    it('должен возвращать allday направления для allday событий на дневной сетке', () => {
      const gridItem = new EventRecord({
        start: Number(moment('2000-01-01')),
        end: Number(moment('2000-01-02'))
      });
      const gridView = 'day';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.allday);
    });
  });

  describe(`вычисление доступных направлений popup'а для групп дел`, () => {
    it('должен возвращать default направления для групп дел на месячной сетке', () => {
      const gridItem = new TodoGroup();
      const gridView = 'month';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.default);
    });

    it('должен возвращать allday направления для групп дел на недельной сетке', () => {
      const gridItem = new TodoGroup();
      const gridView = 'week';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.allday);
    });

    it('должен возвращать allday направления для групп дел на дневной сетке', () => {
      const gridItem = new TodoGroup();
      const gridView = 'day';

      const props = getGridItemPopupProps({gridItem, gridView});

      expect(props.directions).toEqual(POPUP_DIRECTIONS.allday);
    });
  });

  it('должен бросать исключение для неизвестного элемента сетки', () => {
    const gridItem = 'boom';
    const gridView = 'month';
    const pointerPos = {top: 100, left: 100};
    const anchor = document.createElement('div');

    const target = () => getGridItemPopupProps({gridItem, gridView, pointerPos, anchor});

    expect(target).toThrowError(`Unknown grid item 'boom'`);
  });
});
