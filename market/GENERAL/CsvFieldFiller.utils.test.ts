import { getCsvInfo } from './CsvFieldFiller.utils';
import { CsvFieldFillerPluginConfig } from './CsvFieldFiller.types';
import { PARENT_ID_FIELD, ROW_ID_FIELD } from './CsvFieldFiller.constants';

const customNodeTypeId =
  '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FNavigationTreeWithConfig%2Findex%2FCmsNavNodeConfig';

const customNodeTypeId2 =
  '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FNavigationTreeWithConfig%2Findex%2FCmsNavNodeConfig123';

describe('CsvFieldFiller.utils', () => {
  const entryInfo = {
    parent: {
      id: '105851158',
      fieldName: 'navigationTreeProps',
    },
    schemaPath: [
      'CATALOG_MP_DESKTOP_CONTEXT',
      'CONTENT',
      'PAGE_CONTENT_CATALOG_BERU',
      'ROWS',
      'ROW_REACT',
      'COLUMNS',
      'COLUMN_REACT',
      'WIDGETS',
      '%23%2Fdefinitions%2Fwidget%3ASearchLayout',
      'props',
      '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FSearchLayout%2Findex%2FProps',
      'navigationTreeProps',
      '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FSearchLayout%2Findex%2FProps%2FnavigationTreeProps',
    ],
    fieldProperties: {
      nodes: {
        allowedTypes: [
          '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FNavigationTreeWithConfig%2Findex%2FCmsNavNodeConfig',
        ],
        allowedValuesMin: 1,
        label: 'Узел дерева',
        allowedValuesMax: 100,
      },
    },
  };
  const contentTypes = {
    [customNodeTypeId]: {
      type: 'ContentType',
      id: '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FNavigationTreeWithConfig%2Findex%2FCmsNavNodeConfig',
      name: '#/definitions/../src/widgets/content/NavigationTreeWithConfig/index/CmsNavNodeConfig',
      parentNames: [],
      templates: [],
      fields: [
        {
          name: 'alias',
          properties: {
            allowedValuesMin: 0,
            label: 'Отображаемое название узла',
            allowedValuesMax: 1,
          },
        },
        {
          name: 'children',
          properties: {
            allowedTypes: [
              '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FNavigationTreeWithConfig%2Findex%2FCmsNavNodeConfig',
            ],
            allowedValuesMin: 0,
            allowedValuesMax: 200,
            label: 'Дочерний узел',
          },
        },
        {
          name: 'nid',
          properties: {
            allowedValuesMin: 1,
            allowedValuesMax: 1,
          },
        },
      ],
      properties: {
        allowedValuesMax: 200,
        label: 'Дочерний узел',
      },
      selectors: [],
    },
    [customNodeTypeId2]: {
      type: 'ContentType',
      id: '%23%2Fdefinitions%2F..%2Fsrc%2Fwidgets%2Fcontent%2FNavigationTreeWithConfig%2Findex%2FCmsNavNodeConfig123',
      name: '#/definitions/../src/widgets/content/NavigationTreeWithConfig/index/CmsNavNodeConfig123',
      parentNames: [],
      templates: [],
      fields: [
        {
          name: 'alias',
          properties: {
            allowedValuesMin: 0,
            label: 'Отображаемое название узла',
            allowedValuesMax: 1,
          },
        },
        {
          name: 'nid',
          properties: {
            allowedValuesMin: 1,
            allowedValuesMax: 1,
          },
        },
      ],
      properties: {
        allowedValuesMax: 200,
        label: 'Дочерний узел',
      },
      selectors: [],
    },
  };
  const resolve = (path: string) => {
    const fieldName = path.substring(path.lastIndexOf('/') + 1);
    return contentTypes[customNodeTypeId].fields.find(f => f.name === fieldName)?.properties ?? {};
  };

  it('getCsvInfo hierarchical config', () => {
    const config: CsvFieldFillerPluginConfig = {
      data: ['alias', 'nid'],
      transform_rules: [],
      placeholder: 'nodes',
      childPlaceholder: 'children',
      childNodeType: '#/definitions/../src/widgets/content/NavigationTreeWithConfig/index/CmsNavNodeConfig',
    };

    const info = getCsvInfo(config, entryInfo as any, contentTypes as any, resolve);
    expect(info).toEqual({
      fields: [
        { name: ROW_ID_FIELD, label: 'Id строки' },
        { name: PARENT_ID_FIELD, label: 'Id родителя' },
        { name: 'alias', label: 'Отображаемое название узла' },
        { name: 'nid', label: 'nid' },
      ],
      config,
      contentTypeId: customNodeTypeId,
    });
  });

  it('getCsvInfo show error in case nodeType does not have children field', () => {
    const config: CsvFieldFillerPluginConfig = {
      data: ['alias', 'nid'],
      transform_rules: [],
      placeholder: 'nodes',
      childPlaceholder: 'children',
      childNodeType: '#/definitions/../src/widgets/content/NavigationTreeWithConfig/index/CmsNavNodeConfig123',
    };

    const info = getCsvInfo(config, entryInfo as any, contentTypes as any, resolve);
    expect(info).toEqual(
      'В узле "#/definitions/../src/widgets/content/NavigationTreeWithConfig/index/CmsNavNodeConfig123" отсутствует поле "children". Доступные поля: alias, nid'
    );
  });

  it('getCsvInfo plain list config', () => {
    const config: CsvFieldFillerPluginConfig = {
      data: ['alias', 'nid'],
      transform_rules: [],
      placeholder: 'nodes',
    };

    const info = getCsvInfo(config, entryInfo as any, contentTypes as any, resolve);
    expect(info).toEqual({
      fields: [
        { name: 'alias', label: 'Отображаемое название узла' },
        { name: 'nid', label: 'nid' },
      ],
      config,
      contentTypeId: customNodeTypeId,
    });
  });
});
