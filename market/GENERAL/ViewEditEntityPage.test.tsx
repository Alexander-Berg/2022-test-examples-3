import React from 'react';
import { act, fireEvent, render } from '@testing-library/react';

import { ViewEditEntityPage } from './ViewEditEntityPage';
import { setupTestProvider } from 'test/setupApp';
import { BMDM } from 'src/routes/routes';
import { CommonEntityTypeEnum, CommonParamValueType, FindResponse, FrontendConfig } from 'src/java/definitions';
import { Route, Switch } from 'react-router-dom';

const ENTITY_ID = 90404;

const METADATA_RESPONSE = {
  commonEntityType: {
    commonEntityTypeEnum: 'MDM_ENTITY',
    commonParams: [
      {
        commonParamName: 'entity_type_id',
        commonParamValueType: CommonParamValueType.NUMERIC,
        ruTitle: 'Id типа сущности',
        required: false,
        options: [],
        multivalue: false,
      },
      {
        commonParamName: 'version_from',
        commonParamValueType: CommonParamValueType.TIMESTAMP,
        ruTitle: 'Дата начала действия',
        required: false,
        options: [],
        multivalue: false,
      },
      {
        commonParamName: 'version_to',
        commonParamValueType: CommonParamValueType.TIMESTAMP,
        ruTitle: 'Дата окончания действия',
        required: false,
        options: [],
        multivalue: false,
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
      },
      {
        commonParamName: 'categoryId',
        commonParamValueType: CommonParamValueType.INT64,
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
    commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY,
    commonParams: METADATA_RESPONSE.commonEntityType.commonParams,
  },
  commonEntities: [
    {
      entityId: 0,
      commonEntityType: {
        commonEntityTypeEnum: 'MDM_ENTITY',
        commonParams: METADATA_RESPONSE.commonEntityType.commonParams,
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
          versionFrom: 1633003599838,
          versionTo: null,
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
          versionFrom: 1633003599838,
          versionTo: null,
        },
        {
          commonParamName: 'request_entity_id',
          strings: [],
          numerics: [ENTITY_ID],
          booleans: [],
          options: [],
          structs: [],
          timestamps: [],
          references: [],
          int64s: [],
          versionFrom: 1633003599838,
          versionTo: null,
        },
        {
          commonParamName: 'request_common_entity_type',
          strings: [],
          numerics: [],
          booleans: [],
          options: [{ commonEnumId: 8, commonEnumValue: 'MDM_ENTITY' }],
          structs: [],
          timestamps: [],
          references: [],
          int64s: [],
          versionFrom: 1633003599838,
          versionTo: null,
        },
        {
          commonParamName: 'version_from',
          strings: [],
          numerics: [],
          booleans: [],
          options: [],
          structs: [],
          timestamps: [0],
          references: [],
          int64s: [],
          versionFrom: 1633003599838,
          versionTo: null,
        },
        {
          commonParamName: 'version_to',
          strings: [],
          numerics: [],
          booleans: [],
          options: [],
          structs: [],
          timestamps: null,
          references: [],
          int64s: [],
          versionFrom: 1633003599838,
          versionTo: null,
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
          int64s: [ENTITY_ID],
          versionFrom: 0,
          versionTo: null,
        },
      ],
      commonFilter: null,
    },
  ],
};

jest.mock('src/widgets/ConfirmationModal/confirmation-modal.model.ts', () => {
  return {
    ConfirmationModalModel: class A {
      constructor(config: { onApprove: () => void }) {
        config.onApprove();
      }
    },
  };
});

describe('ViewEditEntityPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <ViewEditEntityPage />
      </Provider>
    );
  });

  it('render MDM_ENTITY with int64', async () => {
    const { Provider, api } = setupTestProvider(
      `${BMDM.WIDGET_VIEW_EDIT.base}/${CommonEntityTypeEnum.MDM_ENTITY}/${ENTITY_ID}/34657`
    );
    const app = render(
      <Provider>
        <Switch>
          <Route path={`${BMDM.WIDGET_VIEW_EDIT.path}/:entityTypeId`} component={ViewEditEntityPage} />
        </Switch>
      </Provider>
    );

    await act(async () => {
      api.configController.currentUser.next().resolve({ roles: ['DEVELOPER'], login: 'testik' });
    });

    await act(async () => {
      api.configController.config.next().resolve({} as FrontendConfig);
    });

    await act(async () => {
      api.metadataApiController.find.next().resolve(FIND_RESPONSE as unknown as FindResponse);
    });

    await app.findByText(ENTITY_ID.toString());

    const editButton = app.getByTitle('Редактировать');
    fireEvent.click(editButton);

    await act(async () => {
      api.metadataApiController.find.next().resolve(FIND_RESPONSE as unknown as FindResponse);
    });

    const editedField = (await app.findByText('Id категории'))?.parentElement?.parentElement
      ?.getElementsByTagName('input')
      .item(0);

    expect(editedField).toBeTruthy();

    const newValue = '123456';
    fireEvent.change(editedField!, { target: { value: newValue } });

    fireEvent.click(app.getByText('Сохранить')!);

    expect(api.metadataApiController.save.activeRequests()).toHaveLength(1);
    const requestPayload = api.metadataApiController.save.activeRequests()[0][0];
    const categoryIdRequestInfo = requestPayload.commonEntities[0]?.commonParamValues?.find(
      v => v.commonParamName === 'categoryId'
    );
    expect(categoryIdRequestInfo).toBeTruthy();
    expect(categoryIdRequestInfo!.int64s).toEqual([+newValue]);
  });
});
