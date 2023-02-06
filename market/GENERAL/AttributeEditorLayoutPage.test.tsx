import React from 'react';
import { act, fireEvent, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryHistory } from 'history';
import { Route, Router, Switch } from 'react-router-dom';

import { AttributeEditorLayoutPage } from './AttributeEditorLayoutPage';
import { setupTestProvider } from 'test/setupApp';
import { FindResponse, WidgetType } from 'src/java/definitions';
import { CONSTRUCTOR } from 'src/routes/routes';

describe('AttributeEditorLayoutPage', () => {
  it('renders', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <AttributeEditorLayoutPage />
      </Provider>
    );
  });

  it('check breadcrumbs', async () => {
    const { Provider, api } = setupTestProvider(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6`);
    const app = render(
      <Provider>
        <Route path={`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/:entityTypeId`} component={AttributeEditorLayoutPage} />
      </Provider>
    );

    await act(async () => {
      api.metadataApiController.find
        .next(req => req.widgetType === WidgetType.TREE)
        .resolve(findResults() as unknown as FindResponse);
    });

    fireEvent.click(await app.findByText('Срок гарантии'));

    const breadcrumbs = app.container.getElementsByTagName('li');
    expect(breadcrumbs).toHaveLength(7);
  });

  it('check url params', async () => {
    const { Provider, api } = setupTestProvider(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6`);
    const history = createMemoryHistory();
    history.push(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6`);
    const app = render(
      <Provider>
        <Router history={history}>
          <Switch location={history.location}>
            <Route
              path={`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/:entityTypeId/:selectedAttributeId`}
              component={AttributeEditorLayoutPage}
            />
            <Route path={`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/:entityTypeId`} component={AttributeEditorLayoutPage} />
          </Switch>
        </Router>
      </Provider>
    );

    await act(async () => {
      api.metadataApiController.find
        .next(req => req.widgetType === WidgetType.TREE)
        .resolve(findResults() as unknown as FindResponse);
    });

    await act(async () => {
      api.metadataApiController.find
        .next(req => req.widgetType === WidgetType.TREE)
        .resolve(findResults() as unknown as FindResponse);
    });

    const attr1 = await app.findByText('Срок гарантии');

    userEvent.click(attr1);
    expect(history.location.pathname).toBe(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6/35528`);

    const attr2 = await app.findByText('Срок гарантии2');
    userEvent.click(attr2);
    expect(history.location.pathname).toBe(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6/111111`);

    const entityType = app.getAllByText('Золотая MSKU')[1];
    userEvent.click(entityType!);
    expect(history.location.pathname).toBe(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6`);

    history.goBack();
    expect(history.location.pathname).toBe(`${CONSTRUCTOR.MDM_ENTITY_TYPE.path}/6/111111`);
  });
});

function commonEntityType() {
  return {
    commonEntityType: {
      commonEntityTypeEnum: 'MDM_ENTITY_TYPE',
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
          commonParamName: 'mdm_entity_type_reference',
          commonParamValueType: 'NUMERIC',
        },
        {
          commonParamName: 'request_entity_id',
          commonParamValueType: 'NUMERIC',
        },
        {
          commonParamName: 'request_common_entity_type',
          commonParamValueType: 'ENUM',
          options: [
            {
              commonEnumValue: 'MDM_NAVIGATION_TREE',
              commonEnumId: 0,
            },
            {
              commonEnumValue: 'MDM_NAVIGATION_TREE_NODE',
              commonEnumId: 1,
            },
            {
              commonEnumValue: 'MDM_ENTITY_TYPE',
              commonEnumId: 2,
            },
            {
              commonEnumValue: 'MDM_ATTR',
              commonEnumId: 3,
            },
            {
              commonEnumValue: 'MDM_ENUM_OPTION',
              commonEnumId: 4,
            },
            {
              commonEnumValue: 'MDM_ATTR_EXTERNAL_REFERENCE',
              commonEnumId: 5,
            },
            {
              commonEnumValue: 'MDM_BOOLEAN_EXTERNAL_REFERENCE',
              commonEnumId: 6,
            },
            {
              commonEnumValue: 'MDM_ENUM_OPTION_EXTERNAL_REFERENCE',
              commonEnumId: 7,
            },
            {
              commonEnumValue: 'MDM_ENTITY',
              commonEnumId: 8,
            },
            {
              commonEnumValue: 'COMMON_VIEW_TYPE',
              commonEnumId: 9,
            },
            {
              commonEnumValue: 'COMMON_PARAM_VIEW_SETTING',
              commonEnumId: 10,
            },
            {
              commonEnumValue: 'CATEGORY',
              commonEnumId: 11,
            },
            {
              commonEnumValue: 'SSKU',
              commonEnumId: 12,
            },
            {
              commonEnumValue: 'MSKU',
              commonEnumId: 13,
            },
            {
              commonEnumValue: 'BUSINESS_SSKU',
              commonEnumId: 14,
            },
            {
              commonEnumValue: 'CONTROL_PANEL',
              commonEnumId: 15,
            },
          ],
        },
      ],
    },
  };
}

const attrParams = [
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
    commonParamName: 'mdm_entity_type_reference',
    commonParamValueType: 'NUMERIC',
  },
  {
    commonParamName: 'request_entity_id',
    commonParamValueType: 'NUMERIC',
  },
];

function findResults() {
  return {
    metadata: commonEntityType().commonEntityType,
    commonEntities: [
      {
        entityId: 6,
        commonEntityType: {
          commonEntityTypeEnum: 'MDM_ENTITY_TYPE',
          commonParams: commonEntityType().commonEntityType.commonParams,
        },
        commonParamValues: [
          {
            commonParamName: 'tree_node_id',
            numerics: [0],
          },
          {
            commonParamName: 'tree_node_title',
            strings: ['Золотая MSKU'],
          },
          {
            commonParamName: 'parent_id',
          },
          {
            commonParamName: 'mdm_entity_type_reference',
            numerics: [6],
          },
          {
            commonParamName: 'request_entity_id',
            numerics: [6],
          },
        ],
      },
      {
        entityId: 35528,
        commonEntityType: {
          commonEntityTypeEnum: 'MDM_ATTR',
          commonParams: attrParams,
        },
        commonParamValues: [
          {
            commonParamName: 'tree_node_id',
            numerics: [5],
          },
          {
            commonParamName: 'tree_node_title',
            strings: ['Срок гарантии'],
          },
          {
            commonParamName: 'parent_id',
            numerics: [0],
          },
          {
            commonParamName: 'mdm_entity_type_reference',
            numerics: [6],
          },
          {
            commonParamName: 'request_entity_id',
            numerics: [35528],
          },
        ],
      },
      {
        entityId: 111111,
        commonEntityType: {
          commonEntityTypeEnum: 'MDM_ATTR',
          commonParams: attrParams,
        },
        commonParamValues: [
          {
            commonParamName: 'tree_node_id',
            numerics: [111],
          },
          {
            commonParamName: 'tree_node_title',
            strings: ['Срок гарантии2'],
          },
          {
            commonParamName: 'parent_id',
            numerics: [0],
          },
          {
            commonParamName: 'mdm_entity_type_reference',
            numerics: [6],
          },
          {
            commonParamName: 'request_entity_id',
            numerics: [111111],
          },
        ],
      },
    ],
  };
}
