import React, { FC } from 'react';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { AutoRuleCell, AutoRuleCellProps } from './AutoRuleCell';
import { simpleMapping, categoryData } from 'src/test/data';
import { useDescribeCategoryData, setupWithReatom, useDescribeFilter } from 'src/test/withReatom';
import { ParamMappingRow } from '../../types';
import { Api } from 'src/java/Api';
import { ParamMappingType } from 'src/java/definitions';

const TestApp: FC<AutoRuleCellProps> = ({ row }) => {
  useDescribeFilter({ marketCategoryId: categoryData.hid });
  const cat = useDescribeCategoryData(categoryData);
  return cat ? <AutoRuleCell row={row} /> : null;
};

const getRequestValue = (api: MockedApiObject<Api>) => {
  const requests = (api.allActiveRequests as any).paramMappingControllerV2;
  const requestData = requests[0].requests[0][0];
  return requestData.paramMapping as ParamMappingRow;
};

const forceMapping = { ...simpleMapping, mappingType: ParamMappingType.FORCE_MAPPING };
const directMapping = { ...simpleMapping, mappingType: ParamMappingType.DIRECT };
const noEditableMapping = { ...simpleMapping, editable: false };

describe('AutoRuleCell', () => {
  test('save as FORCE_MAPPING', async () => {
    const { app, api } = setupWithReatom(
      <TestApp row={{ original: directMapping, ...directMapping } as ParamMappingRow} />
    );

    await waitFor(() => {
      userEvent.click(app.getByRole('checkbox'));
    });
    expect((api.allActiveRequests as any).paramMappingControllerV2).toBeTruthy();
    expect(getRequestValue(api).mappingType).toBe(ParamMappingType.FORCE_MAPPING);
  });

  test('uncheck FORCE_MAPPING and save ', async () => {
    const { app, api } = setupWithReatom(
      <TestApp row={{ original: forceMapping, ...forceMapping } as ParamMappingRow} />
    );

    await waitFor(() => {
      userEvent.click(app.getByRole('checkbox'));
    });

    expect((api.allActiveRequests as any).paramMappingControllerV2).toBeTruthy();
    expect(getRequestValue(api).mappingType).toBe(ParamMappingType.MAPPING);
  });

  test('check no editable mapping', async () => {
    const { app } = setupWithReatom(
      <TestApp row={{ original: noEditableMapping, ...noEditableMapping } as ParamMappingRow} />
    );

    await waitFor(() => {
      // для нередактируемых маппингов не должен показываться чекбокс
      expect(app.queryByRole('checkbox')).toBeFalsy();
    });
  });
});
