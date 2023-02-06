import React, { FC } from 'react';

import { MappingShopParams, MappingShopParamsProps } from './MappingShopParams';
import { shopModel, categoryData, simpleMapping } from 'src/test/data';
import {
  setupWithReatom,
  useDescribeAllModels,
  useDescribeCategoryData,
  useDescribeFilterData,
} from 'src/test/withReatom';
import userEvent from '@testing-library/user-event';

const TestApp: FC<MappingShopParamsProps> = props => {
  useDescribeAllModels([shopModel]);
  const currentCategoryData = useDescribeCategoryData(categoryData);
  useDescribeFilterData({ marketCategoryId: shopModel.marketCategoryId });

  return currentCategoryData ? <MappingShopParams {...props} /> : <></>;
};

describe('MappingShopParams', () => {
  const onChange = jest.fn();
  const onDelete = jest.fn();

  test('render', async () => {
    const { app } = setupWithReatom(
      <TestApp models={[shopModel]} mapping={simpleMapping} onChange={onChange} onDelete={onDelete} />
    );

    app.getByText(/vendor/i);
  });

  test('remove mapping', async () => {
    const { app } = setupWithReatom(
      <TestApp models={[shopModel]} mapping={simpleMapping} onChange={onChange} onDelete={onDelete} />
    );
    userEvent.click(app.getByTitle('delete'));
    expect(onDelete.mock.calls.length).toEqual(1);
    jest.clearAllMocks();
  });

  test('no editable mapping', async () => {
    const { app } = setupWithReatom(
      <TestApp
        models={[shopModel]}
        mapping={{ ...simpleMapping, editable: false }}
        onChange={onChange}
        onDelete={onDelete}
      />
    );

    expect(app.queryByTitle('delete')).toBeFalsy();
  });
});
