import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { removeEmpty } from '@yandex-market/mbo-components';

import { ApiModel } from 'src/models/api.model';
import { setupTestProvider } from 'test/setupApp';
import { BackInTimeModel } from 'src/models/back-in-time.model';
import { CommonEntityTypeEnum, CommonParamValueType } from 'src/java/definitions';
import { EntityEditorWidget } from './EntityEditorWidget';
import { EntityEditorWidgetModel } from './models/entity-editor-widget.model';

describe('<EntityEditorWidget />', () => {
  it('send edited hierarchical data', async () => {
    const { api, Provider } = setupTestProvider();
    const onSave = jest.fn();
    const model = new EntityEditorWidgetModel(
      new ApiModel(api, () => 1, new BackInTimeModel()),
      {
        entityType: CommonEntityTypeEnum.MDM_ENTITY,
        entityId: 123,
        entityTypeID: 1,
      },
      onSave
    );
    model.fetchData();

    const app = render(
      <Provider>
        <EntityEditorWidget model={model} />
      </Provider>
    );

    api.metadataApiController.find.next().resolve({
      metadata: {
        commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
        commonParams: response().metadata.commonParams,
      },
      commonEntities: response().commonEntities,
    } as any);

    const heightParam = await app.findByText('Высота');
    const heightInput = heightParam?.parentElement?.parentElement?.getElementsByTagName('input').item(0);
    expect(heightInput).toBeTruthy();

    userEvent.type(heightInput!, '123');

    userEvent.click(app.getByText('Сохранить'));

    // confirm save in popup
    const saveBtns = await screen.findAllByText('Сохранить');
    userEvent.click(saveBtns[1]);

    expect(api.metadataApiController.save.activeRequests()).toHaveLength(1);
    expect(api.metadataApiController.save.activeRequests()[0][0]).toEqual(getSaveRequestEntity());
  });

  it('save multiple edited hierarchical data', async () => {
    const { api, Provider } = setupTestProvider();
    const onSave = jest.fn();
    const model = new EntityEditorWidgetModel(
      new ApiModel(api, () => 1, new BackInTimeModel()),
      {
        entityType: CommonEntityTypeEnum.MDM_ENTITY,
        entityId: 123,
        entityTypeID: 1,
      },
      onSave
    );
    model.fetchData();

    const app = render(
      <Provider>
        <EntityEditorWidget model={model} />
      </Provider>
    );

    api.metadataApiController.find.next().resolve({
      metadata: {
        commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
        commonParams: response(true).metadata.commonParams,
      },
      commonEntities: response(true).commonEntities,
    } as any);

    const heightParams = await app.findAllByText('Высота');

    let heightInput = heightParams[0]?.parentElement?.parentElement?.getElementsByTagName('input').item(0);
    expect(heightInput).toBeTruthy();

    userEvent.type(heightInput!, '123');

    heightInput = heightParams[1]?.parentElement?.parentElement?.getElementsByTagName('input').item(0);
    expect(heightInput).toBeTruthy();

    userEvent.type(heightInput!, '456');

    userEvent.click(app.getByText('Сохранить'));

    // confirm save in popup
    const saveBtns = await screen.findAllByText('Сохранить');
    userEvent.click(saveBtns[1]);

    expect(api.metadataApiController.save.activeRequests()).toHaveLength(1);
    expect(api.metadataApiController.save.activeRequests()[0][0]).toEqual(getSaveRequestEntity(true));
  });

  it('confirm canceling changes', async () => {
    const { api, Provider } = setupTestProvider();
    const onCancel = jest.fn();
    const model = new EntityEditorWidgetModel(
      new ApiModel(api, () => 1, new BackInTimeModel()),
      {
        entityType: CommonEntityTypeEnum.MDM_ENTITY,
        entityId: 123,
        entityTypeID: 1,
      },
      undefined,
      undefined,
      onCancel
    );
    model.fetchData();

    const app = render(
      <Provider>
        <EntityEditorWidget model={model} />
      </Provider>
    );

    api.metadataApiController.find.next().resolve({
      metadata: {
        commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
        commonParams: response().metadata.commonParams,
      },
      commonEntities: response().commonEntities,
    } as any);

    const heightParam = await app.findByText('Высота');
    const heightInput = heightParam?.parentElement?.parentElement?.getElementsByTagName('input').item(0);
    expect(heightInput).toBeTruthy();
    const cancelBtn = app.getByText('Отменить');
    userEvent.click(cancelBtn);
    expect(onCancel).toBeCalledTimes(1);
    userEvent.type(heightInput!, '123');

    userEvent.click(cancelBtn);

    const confrimationCancelBtn = await screen.findByText('Нет');
    userEvent.click(confrimationCancelBtn!);
    expect(onCancel).toBeCalledTimes(1);

    userEvent.click(cancelBtn);
    const confrimationApproveBtn = await screen.findByText('Да');
    userEvent.click(confrimationApproveBtn!);

    expect(onCancel).toBeCalledTimes(2);
  });
});

function getSaveRequestEntity(multipleStructs?: boolean) {
  return {
    commonEntities: [
      {
        commonEntityType: {
          commonEntityTypeEnum: 'MDM_ENTITY',
          commonParams: [],
        },
        commonParamValues: [
          {
            commonParamName: 'id',
            numerics: [1234],
          },
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
            structs: removeEmpty([
              {
                entityId: 456,
                commonEntityType: {
                  commonParams: [
                    {
                      commonParamName: 'length',
                      commonParamValueType: 'NUMERIC',
                      ruTitle: 'Длина',
                    },
                    {
                      commonParamName: 'width',
                      commonParamValueType: 'NUMERIC',
                      ruTitle: 'Ширина',
                    },
                    {
                      commonParamName: 'height',
                      commonParamValueType: 'NUMERIC',
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
                    numerics: [123],
                  },
                ],
              },
              multipleStructs && {
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
                    numerics: [11],
                  },
                  {
                    commonParamName: 'width',
                    numerics: [51],
                  },
                  {
                    commonParamName: 'height',
                    numerics: [456],
                  },
                ],
              },
            ]),
          },
        ],
      },
    ],
    widgetType: 'EDITOR',
    context: {
      commitMessage: '',
      superOperatorOverride: false,
    },
  };
}

function response(multipleStructs?: boolean) {
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
          multivalue: !!multipleStructs,
        },
      ],
    },
    commonEntities: [
      {
        entityId: 1234,
        commonParamValues: [
          {
            commonParamName: 'id',
            numerics: [1234],
          },
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
            structs: removeEmpty([
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
              multipleStructs && {
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
                    numerics: [11],
                  },
                  {
                    commonParamName: 'width',
                    numerics: [51],
                  },
                  {
                    commonParamName: 'height',
                    strings: [21],
                  },
                ],
              },
            ]),
          },
        ],
      },
    ],
  };
}
