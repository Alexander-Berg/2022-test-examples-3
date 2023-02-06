import { ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { getModelUpdateErrorActions } from 'src/tasks/common-logs/store/models/epics/helpers/getModelUpdateErrorActions';
import { getModelUpdateErrors } from 'src/tasks/common-logs/store/models/epics/helpers/getModelUpdateErrors';
import { RootState } from 'src/tasks/common-logs/store/root/reducer';

jest.mock('./getModelUpdateErrors');
const getModelUpdateErrorsMock = getModelUpdateErrors as jest.Mock;
getModelUpdateErrorsMock.mockReturnValue(() => ({}));

const model = { id: 123, currentType: ModelType.GURU, title: 'testModelTitle' } as NormalisedModel;

const state = {
  data: { categoryId: 123 },
  models: {
    normalisedModels: { [model.id]: model },
  },
  submit: {},
} as RootState;

describe('getModelUpdateErrorActions', () => {
  it('work empty', () => {
    expect(getModelUpdateErrorActions(state, {}, { reqId: 'testReqId' })).toEqual([
      { payload: 'models:updating', type: '@@loader//HIDE' },
      { payload: [], type: '@@models//SET_FAILED_MODELS' },
    ]);
  });
});
