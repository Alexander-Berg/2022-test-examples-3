import React, { PropsWithChildren, useEffect } from 'react';
import { withRouter } from 'react-router';
import { MockedRequest, ResponseResolver, rest, restContext } from 'msw';
import { createMemoryHistory } from 'history';
import { act, waitFor } from '@testing-library/react';

import Page from './schema-editor.page';

import { render, server } from '@/test-utils';
import { NamespaceModel } from '@/store/collections/namespace/namespace.model';
import { BranchModel } from '@/store/collections/branch/branch.model';
import { RevisionModel } from '@/store/collections/revision/revision.model';
import { ChangesetInfoModel } from '@/store/collections/changeset-info/changeset-info.model';
import { SchemaEditorMergedSchema } from '@/pages/schema-editor/models';
import { Device, Format } from '@/models';
import { DocumentExportView } from '@/store/collections/document/document.model';
import { ViewerDto, ViewerPermission } from '@/dto';
import { useAction } from '@/utils/libs/react-redux';
import { getViewerAction } from '@/store/viewer';

type RawViewerDto = Omit<ViewerDto, 'permissions'> & {
  roles: ViewerDto['permissions'];
};

const SchemaEditorPage = withRouter(Page);

const revisionId = 194691;
const initialSearch = `?namespace=namespace&branch=branch1&revision=${revisionId}`;
const disabledNamespaceSearch = `?namespace=disabled_namespace&branch=branch1&revision=${revisionId}`;
const history = createMemoryHistory({
  initialEntries: [`/schema${initialSearch}`],
});

const match = {
  path: '/schema',
  url: '/schema',
  isExact: true,
  params: {},
};

const getMockConnectionUserApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: RawViewerDto = {
    lastLogin: '2021-05-26T16:57:40.987377Z',
    avatarId: '0/0-0',
    roles: [
      ViewerPermission.CAN_READ,
      ViewerPermission.CAN_SAVE,
      ViewerPermission.CAN_PUBLISH,
      ViewerPermission.CAN_EDIT_PAGE_HINT,
      ViewerPermission.CAN_EDIT_ROLES,
      ViewerPermission.CAN_SAVE_SCHEMA,
      ViewerPermission.CAN_SAVE_SCHEMA_DOC_TYPE,
      ViewerPermission.CAN_SAVE_SCHEMA_CLIENTS,
    ],
    fullName: 'Testik Testovich Testov',
    id: '9999999999999999999999',
    login: 'loginloginlogin',
  };
  return res(ctx.status(200), ctx.body(data));
};

const getMockConnectionNamespacesApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: NamespaceModel[] = [
    {
      name: 'namespace',
      masterBranchName: 'master',
      service: 'service',
      platform: 'platform',
      disableSchemaEditor: false,
    },
    {
      name: 'disabled_namespace',
      masterBranchName: 'master',
      service: 'service',
      platform: 'platform',
      disableSchemaEditor: true,
    },
  ];
  return res(ctx.status(200), ctx.body(data));
};

const getMockConnectionBranchesApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: BranchModel[] = [
    {
      id: 123,
      name: 'branch1',
      master: false,
      namespace: 'namespace',
      ownerId: 456,
      parentId: 789,
      published: true,
      revisionId: 321,
    },
    {
      id: 987,
      name: 'master',
      master: true,
      namespace: 'namespace',
      ownerId: 456,
      parentId: 789,
      published: true,
      revisionId: 111,
    },
  ];
  return res(ctx.status(200), ctx.body(data));
};

const getMockConnectionRevisionsApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: RevisionModel[] = [
    {
      created: '2021-04-22T00:36:36+0300',
      creatorId: 44296711,
      creatorLogin: 'commince',
      id: revisionId,
      sources: [
        { branchName: 'branch1', revisionId: 194536 },
        { branchName: 'master', revisionId: 194690 },
      ],
    },
  ];
  return res(ctx.status(200), ctx.body(data));
};

const getMockConnectionChangeSetApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: ChangesetInfoModel[] = [
    {
      created: '2021-04-22T00:36:36+0300',
      creatorId: 44296711,
      id: 12345678,
      source: 'editor',
    },
  ];
  return res(ctx.status(200), ctx.body(data));
};

const getMockConnectionMergedSchemaApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (
  req,
  res,
  ctx
) => {
  const data: SchemaEditorMergedSchema = {
    revisionId,
    changesetIds: 12345678,
    nodeTypes: [
      {
        name: 'PRODUCT_CONTENT',
        fields: [
          {
            name: 'ROWS',
            properties: [
              {
                name: 'allowedTypes',
                values: [
                  'ROW_2_320',
                  'ROW_3_250',
                  'ROW_REACT_24',
                  'ROW_SCREEN',
                  'ROW_1_720',
                  'ROW_4_150',
                  'ROW_8_6_2',
                  'ROW_REACT',
                  'ROW_SCREEN_SCREEN',
                ],
              },
              {
                name: 'allowedValuesMin',
                values: ['1'],
              },
              {
                name: 'allowedValuesMax',
                values: ['200'],
              },
              {
                name: 'label',
                values: ['Ряд'],
              },
            ],
          },
        ],
        templates: [
          {
            device: Device.DESKTOP,
            format: Format.JSON,
            template: '{\n  "rows": [__ROWS__]\n}\n',
          },
        ],
        propertiesBranch: [
          {
            path: '[BESTSELLERS]/METRIKA',
            properties: [
              {
                name: 'default_value',
                values: ['METRIKA_POPULAR'],
              },
            ],
          },
          {
            path: 'WIDGETS[WIDGET_IMAGE_LINK]/METRIKA/TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_articles_pic'],
              },
            ],
          },
          {
            path: 'WIDGETS[WIDGET_DISCLAIMER]/METRIKA/TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_artcl_mrkt_src'],
              },
            ],
          },
          {
            path: 'WIDGETS[WIDGET_TEXT]/METRIKA/TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_articles_plain'],
              },
            ],
          },
          {
            path: '[BESTSELLERS]/METRIKA/TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_bestsell_artcls'],
              },
            ],
          },
          {
            path: 'WIDGETS[PRODUCTS_CAROUSEL]/METRIKA/TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_articles_lists'],
              },
            ],
          },
          {
            path: '[BESTSELLERS]/METRIKA/TITLE_TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_articles_populartitle'],
              },
            ],
          },
          {
            path: 'WIDGETS[WIDGET_BUTTON_LINK]/METRIKA/TRACK',
            properties: [
              {
                name: 'default_value',
                values: ['cms_articles_button'],
              },
            ],
          },
        ],
        properties: [
          {
            name: 'export_include__full_',
            values: ['ROWS'],
          },
          {
            name: 'export_include_full',
            values: ['ROWS'],
          },
          {
            name: 'label',
            values: ['Содержимое'],
          },
        ],
      },
    ],
    documents: [
      {
        exports: [
          {
            device: Device.DESKTOP,
            format: Format.JSON,
            view: DocumentExportView.FULL,
            keyTemplates: [
              {
                template: ['device', 'format', 'ds', 'type', 'zoom'],
              },
              {
                template: ['device', 'format', 'ds', 'type', 'page_id', 'zoom'],
                uniq: true,
              },
            ],
            identityFields: ['page_id'],
            client: 'client',
          },
        ],
        nodeVersions: [0],
        rootTemplate: 'CMS_NOTIFICATION',
        type: 'namespace_notification',
        label: 'Уведомление',
        namespace: 'namespace',
      },
    ],
  };
  return res(ctx.status(200), ctx.body(data));
};

describe('<SchemaEditorPage />', () => {
  beforeEach(() => {
    server.use(
      rest.get('/api/jsonPageApi/v1/getUser', getMockConnectionUserApiResponse),
      rest.get('/api/jsonSchemaApi/namespaces', getMockConnectionNamespacesApiResponse),
      rest.get('/api/jsonSchemaApi/namespaces/:namespaceId/branches', getMockConnectionBranchesApiResponse),
      rest.get('/api/jsonSchemaApi/branches/:branchId/revisions', getMockConnectionRevisionsApiResponse),
      rest.get(
        '/api/jsonSchemaApi/branches/:branchId/revisions/:revisionId/changesets',
        getMockConnectionChangeSetApiResponse
      ),
      rest.get(
        '/api/jsonSchemaApi/branches/:branchId/revisions/:revisionId/merged',
        getMockConnectionMergedSchemaApiResponse
      )
    );
  });

  it('should be rendered without errors', () => {
    expect(() => {
      render(<SchemaEditorPage />);
    }).not.toThrow();
  });

  it('should be rendered with disabled schema editor', async () => {
    window.history.pushState({}, '', `/schema${disabledNamespaceSearch}`);
    const app = render(
      <App>
        <Page history={history} location={history.location} match={match} />
      </App>
    );

    await waitFor(() => expect(app.queryByText('Создать новый тип узла')).toBeFalsy());
    expect(app.queryByText('Импортировать слои')).toBeFalsy();
    expect(app.queryByText('Мерж в мастер')).toBeFalsy();
    expect(app.queryByText('Сохранить все')).toBeFalsy();
  });

  it('should load data and navigate back', async () => {
    window.history.pushState({}, '', `/schema${initialSearch}`);
    const app = render(
      <App>
        <Page history={history} location={history.location} match={match} />
      </App>
    );
    await waitFor(jest.fn());
    const tab = await app.findByText('Типы материалов');
    await act(async () => {
      tab.click();
    });
    expect(history.location.search !== `?namespace=namespace&branch=branch1&revision=${revisionId}`);
    await act(async () => {
      history.goBack();
    });
    const nodeTab = app.queryByText('Выберите тип, с которым вы хотите работать.');
    const docTab = app.queryByText('Выберите материал, с которым вы хотите работать.');
    expect(nodeTab).not.toBeNull();
    expect(docTab).toBeNull();
  });
});

function App(props: PropsWithChildren<unknown>) {
  const initUser = useAction(getViewerAction.started);
  useEffect(() => {
    initUser();
  }, [initUser]);

  return <>{props.children}</>;
}
