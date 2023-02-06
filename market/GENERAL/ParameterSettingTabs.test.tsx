import { categoryData, simpleMapping, rule, shopModel } from 'src/test/data';
import {
  resolveDefaultData,
  resolveLoadMappingRequest,
  resolveLoadModelsRequest,
  resolveLoadCategoryDataRequest,
} from 'src/test/api/resolves';
import { setupTestApp } from 'src/test/setupApp';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { setupWithReatom } from 'src/test/withReatom';
import React from 'react';
import { MappingCountTab } from './ParameterSettingTabs';
import { currentCategoryMappingsAtom } from '../../store';

describe('ParameterSettingTabs', () => {
  test('select mapping tab', async () => {
    const { api, app } = setupTestApp(`/parameters?shopId=1&marketCategoryId=${categoryData.hid}`);

    await waitFor(() => expect(api.allActiveRequests).not.toEqual({}));

    resolveDefaultData(api);
    resolveLoadMappingRequest(api, { mappings: [simpleMapping], rules: [rule] });
    resolveLoadModelsRequest(api, [shopModel]);
    resolveLoadCategoryDataRequest(api, categoryData);

    await waitFor(() => expect(api.allActiveRequests).toEqual({}));

    const mapping = app.getByText('Правила заполнения');

    userEvent.click(mapping);

    app.getByText('Характеристики маркетплейса');
  });

  test('MappingCountTab', () => {
    const { app } = setupWithReatom(<MappingCountTab />, { currentCategoryMappingsAtom });
    app.getByText('Правила заполнения');
  });
});
