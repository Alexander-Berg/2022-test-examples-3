import { act, fireEvent, render } from '@testing-library/react';
import React from 'react';

import { setupTestProvider } from 'test/setupApp';
import { ApiModel } from 'src/models/api.model';
import { BackInTimeModel } from 'src/models/back-in-time.model';
import { createDataGridModel } from 'src/utils/factories/createDataGridModel';
import {
  CommonEntityTypeEnum,
  CommonParam,
  CommonParamUiSetting,
  CommonParamValueType,
  FindResponse,
  MetadataResponse,
  WidgetType,
} from 'src/java/definitions';
import { DataGridWidget } from './DataGridWidget';

describe('<DataGridWidget /> resizable columns', () => {
  it('resize columns', async () => {
    const { Provider, api } = setupTestProvider();

    const apiModel = new ApiModel(api, () => Promise.resolve(), new BackInTimeModel());

    const { model } = createDataGridModel(apiModel, {
      entityId: 0,
      entityType: CommonEntityTypeEnum.MDM_ENTITY_TYPE,
    });

    const app = render(
      <Provider>
        <DataGridWidget model={model} />
      </Provider>
    );
    await act(async () => {
      api.metadataApiController.find.next().resolve(getFindResponse());
    });

    const resizeControls = app.getAllByTitle('изменить размер');
    expect(resizeControls).toHaveLength(2);

    expect(() => {
      fireEvent.mouseDown(resizeControls[0]);
      fireEvent.mouseMove(resizeControls[0], { movementX: 50 });
      fireEvent.mouseUp(resizeControls[0]);
    }).not.toThrow();
  });
});

function getFindResponse(): FindResponse {
  const metadataResponse: MetadataResponse = {
    version: 'latest',
    widgetType: WidgetType.TABLE,
    commonEntityType: {
      commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY_TYPE,
      commonParams: [
        {
          commonParamName: 'column1',
          commonParamValueType: CommonParamValueType.STRING,
          multivalue: false,
          options: [],
          required: false,
          ruTitle: 'Колонка 1',
          uiSettings: [CommonParamUiSetting.VISIBLE],
        },
        {
          commonParamName: 'column2',
          commonParamValueType: CommonParamValueType.NUMERIC,
          multivalue: false,
          options: [],
          required: false,
          ruTitle: 'Smirnoff',
          uiSettings: [CommonParamUiSetting.VISIBLE],
        },
      ] as unknown as CommonParam[],
    },
  };

  return {
    metadata: metadataResponse.commonEntityType,
    commonEntities: [
      {
        commonEntityType: metadataResponse.commonEntityType,
        entityId: 111,
        commonParamValues: [
          {
            commonParamName: 'column1',
            strings: ['Значение 1-1'],
          },
          {
            commonParamName: 'column2',
            numerics: [99999],
          },
        ],
      },
      {
        commonEntityType: metadataResponse.commonEntityType,
        entityId: 333,
        commonParamValues: [
          {
            commonParamName: 'column1',
            strings: ['Значение 2-1'],
          },
          {
            commonParamName: 'column2',
            numerics: [777777],
          },
        ],
      },
    ],
  };
}
