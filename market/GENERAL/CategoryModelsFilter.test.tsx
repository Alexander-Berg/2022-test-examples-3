import React, { FC } from 'react';

import { shopModel, categoryData } from 'src/test/data';
import { TestingRouter } from 'src/test/setupApp';
import { setupWithReatom, useDescribeCategoryTree, useDescribeAllModels } from 'src/test/withReatom';
import { CategoryModelsFilter } from './CategoryModelsFilter';
import { ProcessingStatus } from 'src/java/definitions';

const CHANGEABLE_MODELS_URL = '/categories?canChangeCategory=1';

const TestApp: FC<{ route: string }> = ({ route }) => {
  useDescribeCategoryTree({ [categoryData.hid]: categoryData });
  useDescribeAllModels([{ ...shopModel, processingStatus: ProcessingStatus.NEED_CONTENT }]);
  return (
    // eslint-disable-next-line react/jsx-curly-brace-presence
    <TestingRouter route={route}>
      <CategoryModelsFilter />
    </TestingRouter>
  );
};

describe('CategoryModelsFilter', () => {
  test('render', () => {
    setupWithReatom(<TestApp route={CHANGEABLE_MODELS_URL} />);
  });
});
