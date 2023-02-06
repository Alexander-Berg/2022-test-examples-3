import React from 'react';
import { act, render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'test/setupApp';
import { ARM_DS } from 'src/routes/routes';
import { ArmDsLayoutPage } from './ArmDsLayoutPage';
import { Route, Switch } from 'react-router-dom';
import { CommonParamUiSetting, CommonParamValueType, FindResponse, WidgetType } from 'src/java/definitions';

describe('<ArmDsLayoutPage />', () => {
  it('load data and render', async () => {
    const { Provider, api } = setupTestProvider(`${ARM_DS.BASE.path}/0`);
    const app = render(
      <Provider>
        <Switch>
          <Route path={`${ARM_DS.BASE.path}/:selectedNodeId`} component={ArmDsLayoutPage} />
        </Switch>
      </Provider>
    );

    await act(async () => {
      api.metadataApiController.find.next(req => req.widgetType === WidgetType.TREE).resolve(getTreeFindResponse());
    });

    const rootTitle = await app.findAllByText('ARM');
    const treeNode = await app.findByText('Категорийные настройки');

    expect(rootTitle).toHaveLength(2);
    expect(treeNode).toBeInTheDocument();
  });

  it('open entities from url', async () => {
    const { Provider } = setupTestProvider(`${ARM_DS.BASE.path}/123?entities=123%2F456,123%2F789,111%2F222`);
    const app = render(
      <Provider>
        <Switch>
          <Route path={`${ARM_DS.BASE.path}/:selectedNodeId`} component={ArmDsLayoutPage} />
        </Switch>
      </Provider>
    );

    const tabBtns = await app.findAllByTitle('Закрыть');
    const hideDrawerBtn = app.getByTitle('Свернуть все карточки');

    expect(hideDrawerBtn).toBeInTheDocument();
    expect(tabBtns).toHaveLength(3);
  });

  it('close all entities', async () => {
    const { Provider, rootModel } = setupTestProvider(`${ARM_DS.BASE.path}/123?entities=123%2F456,123%2F789,111%2F222`);
    const app = render(
      <Provider>
        <Switch>
          <Route path={`${ARM_DS.BASE.path}/:selectedNodeId`} component={ArmDsLayoutPage} />
        </Switch>
      </Provider>
    );

    const closeAllBtn = app.getByTitle('Закрыть все карточки');
    userEvent.click(closeAllBtn);

    expect(rootModel.armDsLayoutPageModel.entitiesDetailsWidget.entitiesModels.size).toBe(0);
    waitFor(() => expect(app.queryByTitle('Закрыть все карточки')).toBeFalsy());
  });

  it('fail to load data', async () => {
    const { Provider, api } = setupTestProvider(`${ARM_DS.BASE.path}/123`);
    const app = render(
      <Provider>
        <Switch>
          <Route path={`${ARM_DS.BASE.path}/:selectedNodeId`} component={ArmDsLayoutPage} />
        </Switch>
      </Provider>
    );

    await act(async () => {
      api.metadataApiController.find.next(req => req.widgetType === WidgetType.TREE).resolve(getTreeFindResponse());
    });

    await act(async () => {
      api.metadataApiController.find.next(req => req.widgetType === WidgetType.TABLE).reject('no');
    });

    await act(async () => {
      api.metadataApiController.metadata
        .next(req => req.widgetType === WidgetType.TABLE)
        .resolve({
          commonEntityType: {
            commonParams: [
              {
                commonParamName: 'qwerty',
                commonParamValueType: CommonParamValueType.STRING,
                ruTitle: 'Qwerty',
                uiSettings: [CommonParamUiSetting.VISIBLE],
              },
            ],
          },
        });
    });

    expect(await app.findByText('Qwer', { exact: false })).toBeInTheDocument();
  });
});

function getTreeFindResponse() {
  return {
    metadata: {
      commonEntityTypeEnum: 'MDM_ENTITY',
      commonParams: [
        {
          commonParamName: 'tree_node_id',
          commonParamValueType: 'NUMERIC',
        },
        {
          commonParamName: 'tree_node_title',
          commonParamValueType: 'STRING',
        },
        {
          commonParamName: 'parent_id',
          commonParamValueType: 'NUMERIC',
        },
        {
          commonParamName: 'request_entity_id',
          commonParamValueType: 'NUMERIC',
        },
      ],
    },
    commonEntities: [
      {
        entityId: 0,
        commonEntityType: {
          commonEntityTypeEnum: 'MDM_ENTITY',
          commonParams: [
            {
              commonParamName: 'tree_node_id',
              commonParamValueType: 'NUMERIC',
            },
            {
              commonParamName: 'tree_node_title',
              commonParamValueType: 'STRING',
            },
            {
              commonParamName: 'parent_id',
              commonParamValueType: 'NUMERIC',
            },
            {
              commonParamName: 'request_entity_id',
              commonParamValueType: 'NUMERIC',
            },
            {
              commonParamName: 'request_common_entity_type',
              commonParamValueType: 'ENUM',
              options: [{ commonEnumId: 0, commonEnumValue: 'MDM_NAVIGATION_TREE' }],
            },
          ],
        },
        commonParamValues: [
          {
            commonParamName: 'tree_node_id',
            numerics: [0],
            versionFrom: 1639681098676,
          },
          {
            commonParamName: 'tree_node_title',
            strings: ['ARM'],
            versionFrom: 1639681098676,
          },
          {
            commonParamName: 'parent_id',
            numerics: [],
            versionFrom: 1639681098676,
          },
        ],
        commonFilter: null,
      },
      {
        entityId: 34657,
        commonEntityType: {
          commonEntityTypeEnum: 'MDM_ENTITY',
          commonParams: [
            {
              commonParamName: 'tree_node_id',
              commonParamValueType: 'NUMERIC',
            },
            {
              commonParamName: 'tree_node_title',
              commonParamValueType: 'STRING',
            },
            {
              commonParamName: 'parent_id',
              commonParamValueType: 'NUMERIC',
            },
            {
              commonParamName: 'request_entity_id',
              commonParamValueType: 'NUMERIC',
            },
            {
              commonParamName: 'request_common_entity_type',
              commonParamValueType: 'ENUM',
              options: [{ commonEnumId: 0, commonEnumValue: 'MDM_NAVIGATION_TREE' }],
            },
          ],
        },
        commonParamValues: [
          {
            commonParamName: 'tree_node_id',
            numerics: [1],
            versionFrom: 1639681098695,
          },
          {
            commonParamName: 'tree_node_title',
            strings: ['Категорийные настройки'],
            versionFrom: 1639681098695,
          },
          {
            commonParamName: 'parent_id',
            numerics: [0],
            versionFrom: 1639681098695,
          },
          {
            commonParamName: 'mdm_entity_type_reference',
            numerics: [34657],
            versionFrom: 1639681098695,
          },
          {
            commonParamName: 'request_entity_id',
            numerics: [34657],
            versionFrom: 1639681098695,
          },
          {
            commonParamName: 'request_common_entity_type',
            options: [{ commonEnumId: 8, commonEnumValue: 'MDM_ENTITY' }],
            versionFrom: 1639681098695,
          },
        ],
        commonFilter: null,
      },
    ],
  } as unknown as FindResponse;
}
