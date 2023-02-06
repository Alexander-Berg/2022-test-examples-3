import moment from 'moment';

import GridRecord from '../GridRecord';
import {getGridPeriod} from '../gridSelectors';

describe('gridSelectors', () => {
  describe('getGridPeriod', () => {
    describe('сетка на день', () => {
      test('должен возвращать текущий период', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'day'
          })
        };

        expect(getGridPeriod(state)).toEqual({
          start: Number(moment('2018-05-30')),
          end: Number(moment('2018-05-30'))
        });
      });

      test('должен возвращать текущий период, смещенный на период вперед', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'day'
          })
        };
        const props = {
          delta: 1
        };

        expect(getGridPeriod(state, props)).toEqual({
          start: Number(moment('2018-05-31')),
          end: Number(moment('2018-05-31'))
        });
      });

      test('должен возвращать текущий период, смещенный на период назад', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'day'
          })
        };
        const props = {
          delta: -1
        };

        expect(getGridPeriod(state, props)).toEqual({
          start: Number(moment('2018-05-29')),
          end: Number(moment('2018-05-29'))
        });
      });
    });

    describe('сетка на неделю', () => {
      test('должен возвращать текущий период', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'week'
          })
        };

        expect(getGridPeriod(state)).toEqual({
          start: Number(moment('2018-05-28')),
          end: Number(moment('2018-06-03'))
        });
      });

      test('должен возвращать текущий период, смещенный на период вперед', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'week'
          })
        };
        const props = {
          delta: 1
        };

        expect(getGridPeriod(state, props)).toEqual({
          start: Number(moment('2018-06-04')),
          end: Number(moment('2018-06-10'))
        });
      });

      test('должен возвращать текущий период, смещенный на период назад', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'week'
          })
        };
        const props = {
          delta: -1
        };

        expect(getGridPeriod(state, props)).toEqual({
          start: Number(moment('2018-05-21')),
          end: Number(moment('2018-05-27'))
        });
      });
    });

    describe('сетка на месяц', () => {
      test('должен возвращать текущий период', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'month'
          })
        };

        expect(getGridPeriod(state)).toEqual({
          start: Number(moment('2018-04-30')),
          end: Number(moment('2018-06-03'))
        });
      });

      test('должен возвращать текущий период, смещенный на период вперед', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'month'
          })
        };
        const props = {
          delta: 1
        };

        expect(getGridPeriod(state, props)).toEqual({
          start: Number(moment('2018-05-28')),
          end: Number(moment('2018-07-01'))
        });
      });

      test('должен возвращать текущий период, смещенный на период назад', () => {
        const state = {
          grid: new GridRecord({
            showDate: Number(moment('2018-05-30')),
            currentView: 'month'
          })
        };
        const props = {
          delta: -1
        };

        expect(getGridPeriod(state, props)).toEqual({
          start: Number(moment('2018-03-26')),
          end: Number(moment('2018-05-06'))
        });
      });
    });
  });
});
