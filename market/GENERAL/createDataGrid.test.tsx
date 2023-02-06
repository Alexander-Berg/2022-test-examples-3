import React, { useContext } from 'react';
import { act, fireEvent, render, RenderResult } from '@testing-library/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import userEvent from '@testing-library/user-event';

import { createDataGridModel } from './createDataGridModel';
import { setupTestProvider } from 'test/setupApp';
import { RootModelContext } from 'src/models/root.model';
import {
  CommonEntity,
  CommonEntityTypeEnum,
  CommonParam,
  CommonParamUiSetting,
  CommonParamValueType,
  FindResponse,
} from 'src/java/definitions';
import Api from 'src/Api';
import { DataGridWidget } from 'src/widgets';

const METADATA_RESPONSE = {
  widgetType: 'TABLE',
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

const ENTITY_FIND_RESPONSE = {
  metadata: {
    ...METADATA_RESPONSE.commonEntityType,
  },
  commonEntities: generateCommonEntities(21),
};

let app: RenderResult;
let mockedApi: MockedApiObject<Api>;

jest.setTimeout(10000);
describe('createDataGrid server-side', () => {
  beforeEach(() => {
    const testProvider = setupTestProvider();
    mockedApi = testProvider.api;
    const { Provider } = testProvider;

    const Component = () => {
      const { apiModel } = useContext(RootModelContext);
      const dataGrid = createDataGridModel(
        apiModel,
        {
          entityType: CommonEntityTypeEnum.MDM_ENTITY,
          entityId: 34657,
          entityTypeID: 456,
        },
        undefined,
        true
      );

      const { model } = dataGrid;
      return <DataGridWidget model={model} />;
    };

    app = render(
      <Provider>
        <Component />
      </Provider>
    );
  });

  it('pagination', async () => {
    await resolveFindResponse(mockedApi);

    const nextPageBtn = await app.findByTitle('Следующая страница');
    fireEvent.click(nextPageBtn);

    expect(mockedApi.metadataApiController.find.activeRequests()).toHaveLength(1);
    let requestWithPagination = mockedApi.metadataApiController.find.activeRequests()[0];
    expect(requestWithPagination[0].commonEntity.commonFilter?.paginationCondition).toMatchObject({
      offset: 20,
      pageSize: 20,
    });
    await resolveFindResponse(mockedApi);

    const prevPageBtn = await app.findByTitle('Предыдущая страница');
    fireEvent.click(prevPageBtn);

    expect(mockedApi.metadataApiController.find.activeRequests()).toHaveLength(1);
    [requestWithPagination] = mockedApi.metadataApiController.find.activeRequests();
    expect(requestWithPagination[0].commonEntity.commonFilter?.paginationCondition).toMatchObject({
      offset: 0,
      pageSize: 20,
    });
  });

  it('render custom import button', async () => {
    await resolveFindResponse(mockedApi);

    await app.findByText('Импорт');
    await app.findByText('Экспорт');

    const fileInput = app.container.querySelector('input[type="file"]') as HTMLInputElement;
    userEvent.upload(fileInput, new File([], ''));

    expect(mockedApi.categoryParamValuesController.importFromExcel).toBeCalledTimes(1);
  });
});

async function resolveFindResponse(api: MockedApiObject<Api>) {
  await act(async () => {
    api.metadataApiController.find.next().resolve(ENTITY_FIND_RESPONSE as unknown as FindResponse);
  });
}

function generateCommonEntities(count: number): CommonEntity[] {
  const entities: CommonEntity[] = [];
  for (let i = 0; i < count; i++) {
    entities.push({
      entityId: 0,
      commonEntityType: {
        commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
        commonParams: [
          {
            commonParamName: 'entity_type_id',
            commonParamValueType: CommonParamValueType.NUMERIC,
            ruTitle: 'Id типа сущности',
            required: false,
            options: [],
            multivalue: false,
            uiSettings: [CommonParamUiSetting.READ_ONLY, CommonParamUiSetting.SEARCHABLE],
          },
          {
            commonParamName: 'version_from',
            commonParamValueType: CommonParamValueType.TIMESTAMP,
            ruTitle: 'Дата начала действия',
            required: false,
            options: [],
            multivalue: false,
            uiSettings: [CommonParamUiSetting.READ_ONLY, CommonParamUiSetting.SEARCHABLE],
          },
          {
            commonParamName: 'version_to',
            commonParamValueType: CommonParamValueType.TIMESTAMP,
            ruTitle: 'Дата окончания действия',
            required: false,
            options: [],
            multivalue: false,
            uiSettings: [CommonParamUiSetting.READ_ONLY, CommonParamUiSetting.SEARCHABLE],
          },
          {
            commonParamName: 'version_status',
            commonParamValueType: CommonParamValueType.ENUM,
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
            uiSettings: [CommonParamUiSetting.READ_ONLY, CommonParamUiSetting.SEARCHABLE],
          },
          {
            commonParamName: 'categoryId',
            commonParamValueType: CommonParamValueType.INT64,
            ruTitle: 'Id категории',
            required: false,
            options: [],
            multivalue: false,
            uiSettings: [CommonParamUiSetting.SEARCHABLE],
          },
        ] as CommonParam[],
      },
      commonParamValues: [
        {
          commonParamName: 'entity_type_id',
          strings: [],
          numerics: [34657],
          booleans: [],
          options: [],
          structs: [],
          timestamps: [],
          references: [],
          int64s: [],
          versionFrom: 1634828316354,
          versionTo: undefined,
        },
        {
          commonParamName: 'version_status',
          strings: [],
          numerics: [],
          booleans: [],
          options: [{ commonEnumId: 0, commonEnumValue: 'ACTIVE' }],
          structs: [],
          timestamps: [],
          references: [],
          int64s: [],
          versionFrom: 1634828316354,
          versionTo: undefined,
        },
        {
          commonParamName: 'request_entity_id',
          strings: [],
          numerics: [i],
          booleans: [],
          options: [],
          structs: [],
          timestamps: [],
          references: [],
          int64s: [],
          versionFrom: 1634828316354,
          versionTo: undefined,
        },
        {
          commonParamName: 'categoryId',
          strings: [],
          numerics: [],
          booleans: [],
          options: [],
          structs: [],
          timestamps: [],
          references: [],
          int64s: [123333333],
          versionFrom: 1634828316354,
          versionTo: undefined,
        },
      ],
    });
  }
  return entities;
}
