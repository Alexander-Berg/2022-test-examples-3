import { ParameterId } from '@yandex-market/mbo-parameter-editor/es/entities/parameter/types';
import { ParameterValue } from '@yandex-market/mbo-parameter-editor/es/entities/parameterValue/types';
import { CategoryData } from '@yandex-market/mbo-parameter-editor/es/entities/categoryData/types';

import {
  testCategoryData,
  testMdmParamMetadata,
  testMdmSsku,
  testModelData,
  backConvertedSskuFromEditor,
} from 'test/data/mdmSskuParameterEditor';
import { convertMdmMetaToEditorCategoryData, convertMdmParamsToEditorModelData } from '../mdm-utils';

import { convertEditorParamValuesToMdmSskuParams } from './utils';

describe('Convert tools', () => {
  it('convertMdmMetaToEditorCategoryData works ok', () => {
    expect(convertMdmMetaToEditorCategoryData(testMdmParamMetadata)).toMatchObject(testCategoryData);
  });

  it('convertMdmParamsToEditorModelData works ok', () => {
    expect(convertMdmParamsToEditorModelData(testMdmSsku.params)).toMatchObject(testModelData);
  });

  it('convertEditorParamValuesToMdmSskuParams works ok', () => {
    const backConverted = convertEditorParamValuesToMdmSskuParams(
      testMdmSsku,
      testCategoryData as any as CategoryData,
      testMdmParamMetadata,
      testModelData[1].parameterValues as any as Record<ParameterId, ParameterValue[]>
    );
    expect(backConverted).toMatchObject(backConvertedSskuFromEditor);
  });
});
