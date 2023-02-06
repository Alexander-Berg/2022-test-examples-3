import React from 'react';
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { EntitiesDetailsWidget } from './EntitiesDetailsWidget';
import { EntitiesDetailsWidgetModel } from './entities-details-widget.model';
import { ApiModel } from 'src/models/api.model';
import { setupTestProvider } from 'test/setupApp';
import { BackInTimeModel } from 'src/models/back-in-time.model';
import {
  CommonEntityTypeEnum,
  CommonParam,
  CommonParamUiSetting,
  CommonParamValueType,
  FindResponse,
  WidgetType,
} from 'src/java/definitions';

describe('<EntitiesDetailsWidget />', () => {
  it('request entity', () => {
    const { api } = setupTestProvider();
    const apiModel = new ApiModel(api, () => 1, new BackInTimeModel());
    const model = new EntitiesDetailsWidgetModel(apiModel);
    const app = render(<EntitiesDetailsWidget model={model} />);

    const nothingSelectedCaption = app.getByText('Выберите объекты');
    expect(nothingSelectedCaption).toBeInTheDocument();

    act(() => {
      model.addEntity(123, 456, CommonEntityTypeEnum.MDM_ENTITY);
    });
    expect(api.metadataApiController.find.activeRequests()).toHaveLength(2); // entity and its relations
  });

  it("render entity's title", async () => {
    const { api } = setupTestProvider();
    const apiModel = new ApiModel(api, () => 1, new BackInTimeModel());
    const model = new EntitiesDetailsWidgetModel(apiModel);
    const app = render(<EntitiesDetailsWidget model={model} />);

    act(() => {
      model.addEntity(123, 456, CommonEntityTypeEnum.MDM_ENTITY);
    });
    await act(async () => {
      api.metadataApiController.find.next().resolve(getFindResponse());
    });

    const paramValue = app.getAllByText('Header Title');
    expect(paramValue).toHaveLength(1);
  });

  it('open link to other entity', async () => {
    const { api } = setupTestProvider();
    const apiModel = new ApiModel(api, () => 1, new BackInTimeModel());
    const model = new EntitiesDetailsWidgetModel(apiModel);
    const app = render(<EntitiesDetailsWidget model={model} />);

    act(() => {
      model.addEntity(123, 456, CommonEntityTypeEnum.MDM_ENTITY);
    });
    await act(async () => {
      api.metadataApiController.find.next().resolve(getFindResponse());
      api.metadataApiController.find.next(req => req.widgetType === WidgetType.TABLE).resolve(getRelationsResponse());
    });

    await act(async () => {
      api.metadataApiController.find
        .next(req => req.commonEntity.commonEntityType.commonEntityTypeEnum === CommonEntityTypeEnum.MDM_RELATION)
        .resolve(getRelationsResponse());
    });

    const entityLink = app.getByTitle('Открыть карточку 999');
    userEvent.click(entityLink);

    expect(app.getAllByTitle('Закрыть')).toHaveLength(2);
    // send request for a new entity (props and relations types)
    expect(api.metadataApiController.find.activeRequests()).toHaveLength(2); // entity and its relations
  });
});

function getRelationsResponse() {
  return {
    commonEntities: [
      {
        commonEntityType: {
          commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
          commonParams: [
            {
              commonParamName: 'param2',
              commonParamValueType: CommonParamValueType.REFERENCE,
              uiSettings: [CommonParamUiSetting.VISIBLE],
              ruTitle: 'entity link',
            },
          ] as CommonParam[],
        },
        commonParamValues: [{ commonParamName: 'param2', references: [{ mdmId: 999, mdmEntityTypeId: 888 }] }],
        entityId: 444,
      },
    ],
    metadata: {
      commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
      commonParams: [
        {
          commonParamName: 'param2',
          commonParamValueType: CommonParamValueType.REFERENCE,
          uiSettings: [CommonParamUiSetting.VISIBLE],
          ruTitle: 'entity link',
        },
      ] as CommonParam[],
    },
  } as FindResponse;
}

function getFindResponse() {
  return {
    commonEntities: [
      {
        commonEntityType: {
          commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
          commonParams: [
            {
              commonParamName: 'param1',
              commonParamValueType: CommonParamValueType.STRING,
              uiSettings: [CommonParamUiSetting.HEADER],
            },
          ] as CommonParam[],
        },
        commonParamValues: [{ commonParamName: 'param1', strings: ['Header Title'] }],
        entityId: 123,
      },
    ],
    metadata: {
      commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
      commonParams: [
        {
          commonParamName: 'param1',
          commonParamValueType: CommonParamValueType.STRING,
          uiSettings: [CommonParamUiSetting.HEADER],
          ruTitle: 'param1',
        },
      ] as CommonParam[],
    },
  } as FindResponse;
}
