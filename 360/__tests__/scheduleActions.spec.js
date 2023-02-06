import * as actionCreators from '../scheduleActions';
import {ActionTypes} from '../scheduleConstants';

describe('scheduleActions', () => {
  describe('updateRange', () => {
    test('должен возвращать экшн UPDATE_RANGE', () => {
      const start = Date.now();
      const end = start + 10000;

      expect(actionCreators.updateRange(start, end)).toEqual({
        type: ActionTypes.UPDATE_RANGE,
        start,
        end
      });
    });
  });
  describe('updateRangeStart', () => {
    test('должен возвращать экшн UPDATE_RANGE_START', () => {
      expect(actionCreators.updateRangeStart()).toEqual({type: ActionTypes.UPDATE_RANGE_START});
    });
  });
  describe('updateRangeDone', () => {
    test('должен возвращать экшн UPDATE_RANGE_DONE', () => {
      expect(actionCreators.updateRangeDone()).toEqual({
        type: ActionTypes.UPDATE_RANGE_DONE
      });
    });
  });
  describe('updateRangeSuccess', () => {
    test('должен возвращать экшн UPDATE_RANGE_SUCCESS', () => {
      const start = Date.now();
      const end = start + 10000;

      expect(actionCreators.updateRangeSuccess(start, end)).toEqual({
        type: ActionTypes.UPDATE_RANGE_SUCCESS,
        start,
        end
      });
    });
  });
  describe('extendRangeUp', () => {
    test('должен возвращать экшн EXTEND_RANGE_UP', () => {
      expect(actionCreators.extendRangeUp()).toEqual({type: ActionTypes.EXTEND_RANGE_UP});
    });
  });
  describe('extendRangeUpStart', () => {
    test('должен возвращать экшн EXTEND_RANGE_UP_START', () => {
      expect(actionCreators.extendRangeUpStart()).toEqual({
        type: ActionTypes.EXTEND_RANGE_UP_START
      });
    });
  });
  describe('extendRangeUpDone', () => {
    test('должен возвращать экшн EXTEND_RANGE_UP_DONE', () => {
      expect(actionCreators.extendRangeUpDone()).toEqual({
        type: ActionTypes.EXTEND_RANGE_UP_DONE
      });
    });
  });
  describe('extendRangeUpSuccess', () => {
    test('должен возвращать экшн EXTEND_RANGE_UP_SUCCESS', () => {
      const start = Date.now();

      expect(actionCreators.extendRangeUpSuccess(start)).toEqual({
        type: ActionTypes.EXTEND_RANGE_UP_SUCCESS,
        start
      });
    });
  });
  describe('extendRangeDown', () => {
    test('должен возвращать экшн EXTEND_RANGE_DOWN', () => {
      expect(actionCreators.extendRangeDown()).toEqual({type: ActionTypes.EXTEND_RANGE_DOWN});
    });
  });
  describe('extendRangeDownStart', () => {
    test('должен возвращать экшн EXTEND_RANGE_DOWN_START', () => {
      expect(actionCreators.extendRangeDownStart()).toEqual({
        type: ActionTypes.EXTEND_RANGE_DOWN_START
      });
    });
  });
  describe('extendRangeDownDone', () => {
    test('должен возвращать экшн EXTEND_RANGE_DOWN_DONE', () => {
      expect(actionCreators.extendRangeDownDone()).toEqual({
        type: ActionTypes.EXTEND_RANGE_DOWN_DONE
      });
    });
  });
  describe('extendRangeDownSuccess', () => {
    test('должен возвращать экшн EXTEND_RANGE_DOWN_SUCCESS', () => {
      const end = Date.now();

      expect(actionCreators.extendRangeDownSuccess(end)).toEqual({
        type: ActionTypes.EXTEND_RANGE_DOWN_SUCCESS,
        end
      });
    });
  });
  describe('updateRangeWithShowDate', () => {
    test('должен возвращать экшн UPDATE_RANGE_WITH_SHOW_DATE', () => {
      expect(actionCreators.updateRangeWithShowDate()).toEqual({
        type: ActionTypes.UPDATE_RANGE_WITH_SHOW_DATE
      });
    });
  });
  describe('setShowDate', () => {
    test('должен возвращать экшн SET_SHOW_DATE', () => {
      const showDate = Date.now();

      expect(actionCreators.setShowDate(showDate)).toEqual({
        type: ActionTypes.SET_SHOW_DATE,
        payload: {
          showDate
        }
      });
    });
  });
});
