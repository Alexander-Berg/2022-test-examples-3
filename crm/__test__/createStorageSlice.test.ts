import { createStore } from 'redux';
import { ETypeString } from 'types/entities';
import { IssueTimelineUpdateData } from 'modules/xiva/types/IssueTimelineUpdateData';
import createStorageSlice from '../createStorageSlice';
import { StorageData } from '../../../types';

const createTimelineV2Data = () => {
  return ({
    issueId: 1,
    meta: {},
    data: [
      {
        etype: ETypeString.YcCall,
        eid: 2,
        data: {
          key: 'value',
        },
      },
    ],
  } as unknown) as IssueTimelineUpdateData;
};

describe('createStorageSlice', () => {
  describe('updateFromTimelineV2Data', () => {
    const storageSlice = createStorageSlice({ name: 'root' });
    const reducer = storageSlice.reducer;

    describe('when has no issue', () => {
      it('does not change state', () => {
        const store = createStore(reducer);

        store.dispatch(storageSlice.actions.updateFromTimelineV2Data(createTimelineV2Data()));

        expect(store.getState()).toMatchSnapshot();
      });
    });

    describe('when has no map[0]', () => {
      it('does not change state', () => {
        const store = createStore(reducer, ({
          nodes: { issues: { 1: { timeline: { map: {} } } } },
        } as unknown) as StorageData);

        store.dispatch(storageSlice.actions.updateFromTimelineV2Data(createTimelineV2Data()));

        expect(store.getState()).toMatchSnapshot();
      });
    });

    describe('when has no new item', () => {
      it('changes state', () => {
        const store = createStore(reducer, ({
          nodes: {
            issues: { 1: { timeline: { map: { 0: { items: ['Mail:0'] }, 'Mail:0': {} } } } },
          },
        } as unknown) as StorageData);

        store.dispatch(storageSlice.actions.updateFromTimelineV2Data(createTimelineV2Data()));

        expect(store.getState()).toMatchSnapshot();
      });
    });

    describe('when already has new item', () => {
      it('does not change state', () => {
        const store = createStore(reducer, ({
          nodes: {
            issues: { 1: { timeline: { map: { 0: { items: ['YcCall:2'] }, 'YcCall:2': {} } } } },
          },
        } as unknown) as StorageData);

        store.dispatch(storageSlice.actions.updateFromTimelineV2Data(createTimelineV2Data()));

        expect(store.getState()).toMatchSnapshot();
      });
    });
  });
});
