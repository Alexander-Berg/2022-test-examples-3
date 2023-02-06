import { getNormalizedCategoryData, getNormalizedModel } from '@yandex-market/mbo-parameter-editor/es';

import { testCategoryProto, TestCategorySetup } from 'src/shared/test-data/test-categories';
import { testModelProto, TestModelSetup } from 'src/shared/test-data/test-models';

export const testModelUi = (setup: TestModelSetup = {}) => getNormalizedModel(testModelProto(setup));
export const testCategoryUi = (setup: TestCategorySetup = {}) => getNormalizedCategoryData(testCategoryProto(setup));
