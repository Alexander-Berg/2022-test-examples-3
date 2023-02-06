import React from 'react';
import { fireEvent, render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ViewEntityPropsWidget } from './ViewEntityPropsWidget';
import { ViewEntityPropsWidgetModel } from './view-entity-props-widget.model';
import { EntityEditorFetcher } from 'src/utils/fetchers/EntityEditorFetcher';
import { ApiModel } from 'src/models/api.model';
import { setupTestProvider } from 'test/setupApp';
import { BackInTimeModel } from 'src/models/back-in-time.model';
import { CommonEntityTypeEnum, CommonParamValueType } from 'src/java/definitions';

const METADATA_RESPONSE = {
  widgetType: 'EDITOR',
  version: 'latest',
  commonEntityType: {
    commonEntityTypeEnum: 'MDM_ENTITY',
    commonParams: [
      {
        commonParamName: 'entity_type_id',
        commonParamValueType: 'NUMERIC',
        ruTitle: 'Id типа сущности',
        required: false,
        options: [],
        multivalue: false,
      },
      {
        commonParamName: 'version_from',
        commonParamValueType: 'TIMESTAMP',
        ruTitle: 'Дата начала действия',
        required: false,
        options: [],
        multivalue: false,
      },
      {
        commonParamName: 'version_to',
        commonParamValueType: 'TIMESTAMP',
        ruTitle: 'Дата окончания действия',
        required: false,
        options: [],
        multivalue: false,
      },
      {
        commonParamName: 'version_status',
        commonParamValueType: 'ENUM',
        ruTitle: 'Статус',
        required: false,
        options: [
          { commonEnumId: 0, commonEnumValue: 'ACTIVE' },
          {
            commonEnumId: 1,
            commonEnumValue: 'RETIRED',
          },
        ],
        multivalue: false,
      },
      {
        commonParamName: 'categoryId',
        commonParamValueType: 'INT64',
        ruTitle: 'Id категории',
        required: false,
        options: [],
        multivalue: false,
      },
    ],
  },
};

const FIND_RESPONSE = {
  metadata: {
    ...METADATA_RESPONSE.commonEntityType,
  },
  commonEntities: [
    {
      commonEntityType: METADATA_RESPONSE.commonEntityType,
      commonParamValues: [
        {
          commonParamName: 'entity_type_id',
          numerics: [1],
        },
        {
          commonParamName: 'version_from',
          timestamps: [12345678797],
        },
        {
          commonParamName: 'version_to',
          timestamps: [],
        },
        {
          commonParamName: 'version_status',
          options: [],
        },
        {
          commonParamName: 'categoryId',
        },
      ],
    },
  ],
};

describe('<ViewEntityPropsWidget />', () => {
  it('check control to show filled values', async () => {
    const { api, Provider } = setupTestProvider();
    const model = new ViewEntityPropsWidgetModel(
      new EntityEditorFetcher(new ApiModel(api, () => 1, new BackInTimeModel()), {
        entityType: CommonEntityTypeEnum.MDM_ENTITY,
        entityId: 123,
        entityTypeID: 1,
      }),
      true
    );
    const app = render(
      <Provider>
        <ViewEntityPropsWidget model={model} />
      </Provider>
    );

    api.metadataApiController.find.next().resolve(FIND_RESPONSE as any);

    const a = await app.findByText('Дата окончания действия');
    expect(a).toBeTruthy();
    expect(app.getByText('Статус')).toBeTruthy();
    expect(app.getByText('Id категории')).toBeTruthy();

    const showOnlyFilledControl = app.getByText('Показать только заполненные');
    fireEvent.click(showOnlyFilledControl);

    expect(app.queryAllByText('Статус')).toHaveLength(0);
    expect(app.queryAllByText('Id категории')).toHaveLength(0);
  });

  it('render hierarchical entities', async () => {
    const { api, Provider } = setupTestProvider();
    const model = new ViewEntityPropsWidgetModel(
      new EntityEditorFetcher(new ApiModel(api, () => 1, new BackInTimeModel()), {
        entityType: CommonEntityTypeEnum.MDM_ENTITY,
        entityId: 123,
        entityTypeID: 1,
      }),
      true
    );
    const app = render(
      <Provider>
        <ViewEntityPropsWidget model={model} />
      </Provider>
    );

    api.metadataApiController.find.next().resolve({
      metadata: {
        commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
        commonParams: response().metadata.commonParams,
      },
      commonEntities: response().commonEntities,
    } as any);

    const wghBlock = await app.findByText('ВГХ');
    const heightParam = app.queryByText('Высота');
    expect(wghBlock).toBeTruthy();
    expect(heightParam).toBeInTheDocument();

    userEvent.click(wghBlock);
    // need to wait until accordion is collapsed
    waitFor(() => expect(heightParam).not.toBeInTheDocument());
    expect(wghBlock).toBeTruthy();
  });
});

function response() {
  return {
    metadata: {
      commonParams: [
        {
          commonParamName: 'id',
          commonParamValueType: CommonParamValueType.NUMERIC,
          ruTitle: 'Id',
        },
        {
          commonParamName: 'supplier_id',
          commonParamValueType: CommonParamValueType.NUMERIC,
          ruTitle: 'SupplierID',
        },
        {
          commonParamName: 'shop_ssku',
          commonParamValueType: CommonParamValueType.STRING,
          ruTitle: 'ShopSsku',
        },
        {
          commonParamName: 'vgh',
          commonParamValueType: CommonParamValueType.STRUCT,
          ruTitle: 'ВГХ',
        },
      ],
    },
    commonEntities: [
      {
        entityId: 1234,
        commonParamValues: [
          {
            commonParamName: 'supplier_id',
            numerics: [555],
          },
          {
            commonParamName: 'shop_ssku',
            strings: ['ABC'],
          },
          {
            commonParamName: 'vgh',
            structs: [
              {
                entityId: 456,
                commonEntityType: {
                  commonParams: [
                    {
                      commonParamName: 'length',
                      commonParamValueType: CommonParamValueType.NUMERIC,
                      ruTitle: 'Длина',
                    },
                    {
                      commonParamName: 'width',
                      commonParamValueType: CommonParamValueType.NUMERIC,
                      ruTitle: 'Ширина',
                    },
                    {
                      commonParamName: 'height',
                      commonParamValueType: CommonParamValueType.NUMERIC,
                      ruTitle: 'Высота',
                    },
                  ],
                },
                commonParamValues: [
                  {
                    commonParamName: 'length',
                    numerics: [10],
                  },
                  {
                    commonParamName: 'width',
                    numerics: [50],
                  },
                  {
                    commonParamName: 'height',
                    strings: [20],
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  };
}
