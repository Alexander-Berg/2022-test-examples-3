import React from 'react';
import userEvent from '@testing-library/user-event';
import { waitFor } from '@testing-library/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { shopModel, parameter } from 'src/test/data';
import { EmptyValueMaker, RESET_VALUE_TEXT, SET_VALUE_TEXT } from './EmptyValueMaker';
import { setupWithReatom } from 'src/test/withReatom';
import { resolveSaveModelsRequest } from 'src/test/api/resolves';
import { ValueSource, ShopModelView } from 'src/java/definitions';
import { Api } from 'src/java/Api';

const emptyMarketValue = {
  parameterId: 7893318,
  ruleId: 0,
  valPos: undefined,
  value: {
    empty: true,
  },
  valueSource: ValueSource.MANUAL,
};

const model = {
  ...shopModel,
  marketValues: {
    [parameter.id]: [emptyMarketValue],
  },
};

const getModelAfterSave = (api: MockedApiObject<Api>) => {
  return (api.allActiveRequests as any).shopModelController[0].requests[0][0][0] as ShopModelView;
};

describe('EmptyValueMaker', () => {
  test('set empty value', async () => {
    const { app, api } = setupWithReatom(<EmptyValueMaker model={shopModel} parameterId={parameter.id} />);
    const btn = app.getByTitle(SET_VALUE_TEXT);
    userEvent.click(btn);

    expect(getModelAfterSave(api).marketValues[parameter.id][0].value.empty).toBeTruthy();

    await waitFor(() => {
      resolveSaveModelsRequest(api, [model]);
    });
  });

  test('reset empty value', async () => {
    const { app, api } = setupWithReatom(<EmptyValueMaker model={model} parameterId={parameter.id} />);
    const btn = app.getByTitle(RESET_VALUE_TEXT);
    userEvent.click(btn);

    expect(getModelAfterSave(api).marketValues[parameter.id].length).toEqual(0);

    await waitFor(() => {
      resolveSaveModelsRequest(api, [shopModel]);
    });
  });
});
