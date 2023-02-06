import { ParameterId } from '@yandex-market/mbo-parameter-editor/es/entities/parameter/types';
import { ParameterValue } from '@yandex-market/mbo-parameter-editor/es/entities/parameterValue/types';
import { CategoryData } from '@yandex-market/mbo-parameter-editor/es/entities/categoryData/types';

import {
  testCategoryData,
  testMdmParamMetadata,
  testMdmMsku,
  testModelData,
  backConvertedMskuFromEditor,
} from 'test/data/mdmMskuParameterEditor';
import { convertMdmMetaToEditorCategoryData, convertMdmParamsToEditorModelData } from '../mdm-utils';

import { convertEditorParamValuesToMdmMskuParams } from './utils';

describe('Convert tools', () => {
  it('convertMdmMetaToEditorCategoryData works ok', () => {
    expect(convertMdmMetaToEditorCategoryData(testMdmParamMetadata)).toMatchObject(testCategoryData);
  });

  it('convertMdmParamsToEditorModelData works ok', () => {
    expect(convertMdmParamsToEditorModelData(testMdmMsku.values)).toMatchObject(testModelData);
  });

  it('convertEditorParamValuesToMdmParams works ok', () => {
    const backConverted = convertEditorParamValuesToMdmMskuParams(
      testMdmMsku,
      testCategoryData as any as CategoryData,
      testMdmParamMetadata,
      testModelData[1].parameterValues as any as Record<ParameterId, ParameterValue[]>
    );
    expect(backConverted).toMatchObject(backConvertedMskuFromEditor);
  });
});
