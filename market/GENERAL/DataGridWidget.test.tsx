import React, { FC } from 'react';
import { act, render, RenderResult, screen } from '@testing-library/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'test/setupApp';
import { createDataGridModel } from 'src/utils/factories/createDataGridModel';
import { ApiModel } from 'src/models/api.model';
import { BackInTimeModel } from 'src/models/back-in-time.model';
import {
  CommonEntityTypeEnum,
  CommonParam,
  CommonParamUiSetting,
  CommonParamValueType,
  FindResponse,
  MetadataResponse,
  WidgetType,
} from 'src/java/definitions';
import Api from 'src/Api';
import { EConditionOperator, EConditionType, IFilterSettings } from 'src/components/ComplexFilter/types';
import { DataGridWidget } from './DataGridWidget';

const TODAY = new Date(2021, 0, 1, 0, 0, 0, 0);

const COMPLEX_FILTERS_CLICK_STR = 'COMPLEX_FILTERS_CLICK_STR';
let mockFilterSettings: IFilterSettings[] = [];

jest.mock('src/components/ComplexFilter/ComplexFilter.tsx', () => {
  return {
    ComplexFilter: ({ onChange }: { onChange?: (s: IFilterSettings[], operators?: EConditionOperator[]) => void }) => {
      function handleChange() {
        onChange?.(mockFilterSettings, ['AND'] as any);
      }

      return <span onClick={handleChange}>{COMPLEX_FILTERS_CLICK_STR}</span>;
    },
  };
});

const metadataResponse: MetadataResponse = {
  version: 'latest',
  widgetType: WidgetType.TABLE,
  commonEntityType: {
    commonEntityTypeEnum: CommonEntityTypeEnum.MDM_ENTITY_TYPE,
    commonParams: [
      {
        commonParamName: 'column1',
        commonParamValueType: CommonParamValueType.STRING,
        multivalue: false,
        options: [],
        required: false,
        ruTitle: 'Колонка 1',
        uiSettings: [CommonParamUiSetting.VISIBLE],
      },
      {
        commonParamName: 'column2',
        commonParamValueType: CommonParamValueType.NUMERIC,
        multivalue: false,
        options: [],
        required: false,
        ruTitle: 'Smirnoff',
        uiSettings: [CommonParamUiSetting.VISIBLE],
      },
      {
        commonParamName: 'column3',
        commonParamValueType: CommonParamValueType.BOOLEAN,
        multivalue: false,
        options: [],
        required: false,
        ruTitle: 'Bool col',
        uiSettings: [CommonParamUiSetting.VISIBLE],
      },
      {
        commonParamName: 'column4',
        commonParamValueType: CommonParamValueType.ENUM,
        multivalue: false,
        options: [
          { commonEnumId: 123, commonEnumValue: 'Testik' },
          { commonEnumId: 456, commonEnumValue: 'Testovich' },
        ],
        required: false,
        ruTitle: 'Enum col',
        uiSettings: [CommonParamUiSetting.VISIBLE],
      },
      {
        commonParamName: 'column5',
        commonParamValueType: CommonParamValueType.TIMESTAMP,
        multivalue: false,
        options: [],
        required: false,
        ruTitle: 'Date col',
        uiSettings: [CommonParamUiSetting.VISIBLE],
      },
    ] as unknown as CommonParam[],
  },
};

const findResponse: FindResponse = {
  metadata: metadataResponse.commonEntityType,
  commonEntities: [
    {
      commonEntityType: metadataResponse.commonEntityType,
      entityId: 111,
      commonParamValues: [
        {
          commonParamName: 'column1',
          strings: ['Значение 1-1'],
        },
        {
          commonParamName: 'column2',
          numerics: [99999],
        },
        {
          commonParamName: 'column3',
          booleans: [true],
        },
        {
          commonParamName: 'column4',
          options: [{ commonEnumId: 456, commonEnumValue: 'Testovich' }],
        },
        {
          commonParamName: 'column5',
          timestamps: [TODAY.getTime()],
        },
      ],
    },
    {
      commonEntityType: metadataResponse.commonEntityType,
      entityId: 333,
      commonParamValues: [
        {
          commonParamName: 'column1',
          strings: ['Значение 2-1'],
        },
        {
          commonParamName: 'column2',
          numerics: [777777],
        },
        {
          commonParamName: 'column3',
          booleans: [false],
        },
        {
          commonParamName: 'column4',
          options: [],
        },
        {
          commonParamName: 'column5',
          timestamps: [TODAY.getTime() - 1000 * 60 * 60 * 48], // 2 previous days
        },
      ],
    },
  ],
};

let api: MockedApiObject<Api>;
let Provider: FC;
let app: RenderResult;

describe('<DataGridWidget />', () => {
  beforeEach(() => {
    const testProvider = setupTestProvider();
    api = testProvider.api;
    Provider = testProvider.Provider;

    const apiModel = new ApiModel(api, () => Promise.resolve(), new BackInTimeModel());

    const { model } = createDataGridModel(apiModel, {
      entityId: 0,
      entityType: CommonEntityTypeEnum.MDM_ENTITY_TYPE,
    });

    app = render(
      <Provider>
        <DataGridWidget model={model} />
      </Provider>
    );

    act(() => {
      api.metadataApiController.find.next().resolve(findResponse);
    });
  });

  it('check datagrid search', () => {
    const searchInput = app.getByPlaceholderText('Поиск...');
    userEvent.type(searchInput, 'Значение 1-1');

    expect(app.queryAllByText('Значение 2-1')).toHaveLength(0);
  });

  it('check datagrid numeric search', () => {
    const searchInput = app.getByPlaceholderText('Поиск...');

    userEvent.type(searchInput, '99999');
    expect(app.queryAllByText('99999')).toHaveLength(1);
    expect(app.queryAllByText('777777')).toHaveLength(0);

    userEvent.click(app.getByTitle('Очистить'));

    expect(app.queryAllByText('99999')).toHaveLength(1);
    expect(app.queryAllByText('777777')).toHaveLength(1);
  });

  it('check string filters', () => {
    openFiltersSettings();

    mockFilterSettings = [
      {
        data: { value: '99999' },
        condition: EConditionType.EQUALS,
        id: metadataResponse.commonEntityType.commonParams![0].commonParamName,
      },
    ];
    const applyFiltersBtn = screen.getByText(COMPLEX_FILTERS_CLICK_STR);
    userEvent.click(applyFiltersBtn);

    expect(app.queryAllByText('99999')).toHaveLength(0);
    expect(app.queryAllByText('777777')).toHaveLength(0);

    mockFilterSettings = [
      {
        data: { value: 'Значение 2' },
        condition: EConditionType.CONTAINS,
        id: metadataResponse.commonEntityType.commonParams![0].commonParamName,
      },
    ];
    userEvent.click(applyFiltersBtn);
    expect(app.queryAllByText('99999')).toHaveLength(0);
    expect(app.queryAllByText('777777')).toHaveLength(1);
  });
});

function openFiltersSettings() {
  const filtersButton = app.getByText('Фильтры');

  userEvent.click(filtersButton!);
}
