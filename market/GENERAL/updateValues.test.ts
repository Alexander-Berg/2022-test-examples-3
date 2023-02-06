import { ImageType, NormalizedImage } from '@yandex-market/mbo-parameter-editor';
import { Parameter } from '@yandex-market/mbo-parameter-editor/es/entities/parameter/types';
import { ParameterValue } from '@yandex-market/mbo-parameter-editor/es/entities/parameterValue/types';

import { ParamsChanges, ParamValuesChangedBy } from 'src/pages/ModelEditorCluster/types/model';
import { updateImageValues, updateParamValues } from './updateValues';

const changedValuesByOur: ParamsChanges = {
  1: {
    changes: {
      conflicts: undefined,
      updates: {
        changedBy: ParamValuesChangedBy.Our,
      },
    },
    param: {} as Parameter,
  },
};

const changedValuesByTheir: ParamsChanges = {
  3: {
    changes: {
      conflicts: undefined,
      updates: {
        changedBy: ParamValuesChangedBy.Their,
      },
    },
    param: {} as Parameter,
  },
};

const theirValues: Record<number, ParameterValue[]> = {
  3: [],
};

const ourValues: Record<number, ParameterValue[]> = {
  1: [],
};

const normalizedImage1: NormalizedImage = {
  type: ImageType.PICTURE,
  url: 'image1',
};

const normalizedImage2: NormalizedImage = {
  type: ImageType.PICTURE,
  url: 'image2',
};

describe('updateValues', () => {
  it('updateParamValues with our values', () => {
    expect(updateParamValues(theirValues, ourValues, changedValuesByOur)).toEqual({
      1: [],
      3: [],
    });
  });

  it('updateParamValues with their values', () => {
    expect(updateParamValues(theirValues, ourValues, changedValuesByTheir)).toEqual({
      3: [],
    });
  });

  it('updateImageValues with their values', () => {
    expect(
      updateImageValues([normalizedImage1], [normalizedImage2], {
        conflicts: undefined,
        updates: {
          changedBy: ParamValuesChangedBy.Their,
        },
      })
    ).toEqual([normalizedImage1]);
  });

  it('updateImageValues with our values', () => {
    expect(
      updateImageValues([normalizedImage1], [normalizedImage2], {
        conflicts: undefined,
        updates: {
          changedBy: ParamValuesChangedBy.Our,
        },
      })
    ).toEqual([normalizedImage2]);
  });
});
