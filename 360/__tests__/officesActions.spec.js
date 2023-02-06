import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';

import {ActionTypes} from '../officesConstants';
import {
  getOffices,
  getOfficesNetwork,
  getOfficesSuccess,
  getOfficesTzOffsets,
  getOfficesTzOffsetsNetwork,
  getOfficesTzOffsetsSuccess
} from '../officesActions';

describe('officesActions', () => {
  describe('getOffices', () => {
    test('должен вернуть экшен GET_OFFICES', () => {
      expect(getOffices()).toEqual({
        type: ActionTypes.GET_OFFICES,
        meta: createActionMetaInfo({network: getOfficesNetwork()})
      });
    });
  });

  describe('getOfficesSuccess', () => {
    test('должен вернуть экшен GET_OFFICES_SUCCESS', () => {
      const offices = [];

      expect(getOfficesSuccess(offices)).toEqual({
        type: ActionTypes.GET_OFFICES_SUCCESS,
        offices
      });
    });
  });

  describe('getOfficesTzOffsets', () => {
    test('должен вернуть экшен GET_OFFICES_TZ_OFFSETS', () => {
      const payload = {
        ts: 1511437623889
      };

      expect(getOfficesTzOffsets(payload)).toEqual({
        type: ActionTypes.GET_OFFICES_TZ_OFFSETS,
        payload,
        meta: createActionMetaInfo({network: getOfficesTzOffsetsNetwork(payload)})
      });
    });
  });

  describe('getOfficesTzOffsetsSuccess', () => {
    test('должен вернуть экшен GET_OFFICES_TZ_OFFSETS_SUCCESS', () => {
      const tzOffsets = {};

      expect(getOfficesTzOffsetsSuccess(tzOffsets)).toEqual({
        type: ActionTypes.GET_OFFICES_TZ_OFFSETS_SUCCESS,
        tzOffsets
      });
    });
  });
});
