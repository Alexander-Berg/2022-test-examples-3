import React from 'react';
import { act, render } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { RelationsWidget } from './RelationsWidget';
import { RelationsWidgetModel } from './relations-widget.model';
import { CommonEntityTypeEnum, CommonParamUiSetting, CommonParamValueType } from '../../java/definitions';
import userEvent from '@testing-library/user-event';

describe('<RelationsWidget />', () => {
  it('renders without errors', async () => {
    const {
      rootModel: { apiModel },
      api,
    } = setupTestProvider();

    const model = new RelationsWidgetModel(apiModel, {
      entityId: 123,
      entityTypeID: 456,
      entityType: CommonEntityTypeEnum.MDM_ENTITY,
    });

    const app = render(<RelationsWidget model={model} />);
    expect(api.metadataApiController.find.activeRequests()).toHaveLength(1);

    await act(async () => {
      api.metadataApiController.find.next().resolve({
        metadata: {
          commonEntityTypeEnum: CommonEntityTypeEnum.MDM_RELATION_TYPE,
          commonParams: [
            {
              commonParamName: 'relType',
              commonParamValueType: CommonParamValueType.NUMERIC,
              uiSettings: [CommonParamUiSetting.VISIBLE, CommonParamUiSetting.HEADER],
              ruTitle: 'relType',
            },
          ],
        },
        commonEntities: [
          {
            entityId: 456,
            commonParamValues: [{ commonParamName: 'relType', numerics: [456] }],
          },
        ],
      });
    });

    await act(async () => {
      api.metadataApiController.find.next().resolve({
        metadata: {
          commonEntityTypeEnum: CommonEntityTypeEnum.MDM_RELATION,
          commonParams: [
            {
              commonParamName: 'qwert',
              commonParamValueType: CommonParamValueType.REFERENCE,
              uiSettings: [CommonParamUiSetting.VISIBLE, CommonParamUiSetting.HEADER],
              ruTitle: 'qwert',
            },
          ],
        },
        commonEntities: [
          {
            entityId: 123,
            commonParamValues: [{ commonParamName: 'qwert', references: [{ mdmEntityTypeId: 1, mdmId: 234512 }] }],
          },
        ],
      });
    });

    userEvent.click(app.getByTitle('Добавить связь'));

    expect(api.metadataApiController.find.activeRequests()).toHaveLength(1);
  });

  it('edit existing relation', async () => {
    const {
      rootModel: { apiModel },
      api,
    } = setupTestProvider();

    const model = new RelationsWidgetModel(apiModel, {
      entityId: 123,
      entityTypeID: 456,
      entityType: CommonEntityTypeEnum.MDM_ENTITY,
    });

    const app = render(<RelationsWidget model={model} />);
    expect(api.metadataApiController.find.activeRequests()).toHaveLength(1);

    await act(async () => {
      api.metadataApiController.find.next().resolve({
        metadata: {
          commonEntityTypeEnum: CommonEntityTypeEnum.MDM_RELATION_TYPE,
          commonParams: [
            {
              commonParamName: 'relType',
              commonParamValueType: CommonParamValueType.NUMERIC,
              uiSettings: [CommonParamUiSetting.VISIBLE, CommonParamUiSetting.HEADER],
              ruTitle: 'relType',
            },
          ],
        },
        commonEntities: [
          {
            entityId: 456,
            commonParamValues: [{ commonParamName: 'relType', numerics: [456] }],
          },
        ],
      });
    });

    await act(async () => {
      api.metadataApiController.find.next().resolve({
        metadata: {
          commonEntityTypeEnum: CommonEntityTypeEnum.MDM_RELATION,
          commonParams: [
            {
              commonParamName: 'qwert',
              commonParamValueType: CommonParamValueType.REFERENCE,
              uiSettings: [CommonParamUiSetting.VISIBLE, CommonParamUiSetting.HEADER],
              ruTitle: 'qwert',
            },
          ],
        },
        commonEntities: [
          {
            entityId: 123,
            commonParamValues: [{ commonParamName: 'qwert', references: [{ mdmEntityTypeId: 1, mdmId: 234512 }] }],
          },
        ],
      });
    });

    userEvent.click(app.getByTitle('Открыть'));

    expect(api.metadataApiController.find.activeRequests()).toHaveLength(1);
  });
});
