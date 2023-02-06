import { Pager, Table, TableBody, TableHead, TableCell, TableRow } from '@yandex-market/mbo-components';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';
import { of, slice } from 'ramda';

import { ColumnSelector } from './components/ExportedRecommendationsTable/components';
import { createInterWarehouseExportedRecommendation } from 'src/test/data/interWarehouseExportedRecommendations';
import { InterWarehouseMovementDetailsPage } from './InterWarehouseMovementDetailsPage';
import { InterWarehouseStatisticsDTO } from 'src/java/definitions-replenishment';
import { columnMap } from './components/ExportedRecommendationsTable/variables';
import { ExportedRecommendationsTable, MovementInfo } from './components';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { movements } from 'src/test/data/interWarehouseMovements';
import { setupTestApp } from 'src/test/setupApp';
import { testUser } from 'src/test/data/user';
import { Loader } from 'src/components';
import Api from 'src/Api';
import {
  DEFAULT_IW_EXPORTED_FIXED_COLUMNS,
  DEFAULT_IW_EXPORTED_STATIC_COLUMNS,
} from './components/ExportedRecommendationsTable/constants';

const firstOf = slice(0, 1);

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const EXPORTED_STATISTICS: InterWarehouseStatisticsDTO = {
  items: 1,
  recommendations: 1,
  sskus: 1,
  sum: 10,
  volume: 32242,
  weight: 5.1,
  suppliers: 0,
};

const OK_FILTER = { interWarehouseMovementIds: [1] };

const EXPORTED_RECOMMENDATION = createInterWarehouseExportedRecommendation();

describe('<InterWarehouseMovementsPage /> (loading)', () => {
  beforeEach(() => {
    ({ app, api } = setupTestApp('/replenishment/inter-warehouse/exported?interWarehouseMovementIds=1'));
    api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
    api.replenishmentUserController.getAll.next().resolve(of(testUser));

    app.update();
  });

  it('Should be loading', () => {
    expect(app.find(InterWarehouseMovementDetailsPage)).toHaveLength(1);
    expect(app.find(ExportedRecommendationsTable)).toHaveLength(1);
    expect(app.find(Loader)).toHaveLength(1);

    expect(api.replenishmentUserController.getAll).toBeCalledTimes(1);
    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalledTimes(1);

    expect(api.interWarehouseRecommendationsController.getExportedInterWarehouseStatistics).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.getExportedInterWarehouseStatistics).toBeCalledWith(OK_FILTER);

    expect(api.interWarehouseRecommendationsController.getInterWarehouseExportedRecommendations).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.getInterWarehouseExportedRecommendations).toBeCalledWith(
      OK_FILTER,
      1,
      100
    );
  });
});

describe('<InterWarehouseMovementsPage /> (loaded)', () => {
  beforeEach(() => {
    ({ app, api } = setupTestApp('/replenishment/inter-warehouse/exported?interWarehouseMovementIds=1'));
    api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
    api.replenishmentUserController.getAll.next().resolve(of(testUser));

    api.interWarehouseMovementsController.recommendations.next().resolve(firstOf(movements));
    api.interWarehouseRecommendationsController.getExportedInterWarehouseStatistics.next().resolve(EXPORTED_STATISTICS);
    api.interWarehouseRecommendationsController.getInterWarehouseExportedRecommendations
      .next()
      .resolve(of(EXPORTED_RECOMMENDATION));

    app.update();
  });

  it('Should mount correctly', () => {
    expect(api.replenishmentUserController.getAll).toBeCalledTimes(1);
    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalledTimes(1);
    expect(api.interWarehouseMovementsController.recommendations).toBeCalledTimes(1);

    expect(api.interWarehouseRecommendationsController.getExportedInterWarehouseStatistics).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.getExportedInterWarehouseStatistics).toBeCalledWith(OK_FILTER);

    expect(api.interWarehouseRecommendationsController.getInterWarehouseExportedRecommendations).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.getInterWarehouseExportedRecommendations).toBeCalledWith(
      OK_FILTER,
      1,
      100
    );

    expect(app.find(InterWarehouseMovementDetailsPage)).toHaveLength(1);
    expect(app.find(ExportedRecommendationsTable)).toHaveLength(1);
    expect(app.find(ColumnSelector)).toHaveLength(1);
    expect(app.find(Pager)).toHaveLength(1);

    const movementInfos = app.find(MovementInfo);
    expect(movementInfos).toHaveLength(1);
    expect(movementInfos.first().text()).toEqual('#1 (tester-daddy), Склад 2   Склад Перемещение в LMS#1');

    expect(app.find(Table)).toHaveLength(2);

    const tableHeaders = app.find(TableHead);
    expect(tableHeaders).toHaveLength(2);

    const fixedColumns = tableHeaders.at(0).find(TableCell);
    expect(fixedColumns).toHaveLength(DEFAULT_IW_EXPORTED_FIXED_COLUMNS.length);
    fixedColumns.forEach((column, index) => {
      expect(column.text()).toEqual(columnMap[DEFAULT_IW_EXPORTED_FIXED_COLUMNS[index]].name);
    });

    const staticColumns = tableHeaders.at(1).find(TableCell);
    expect(staticColumns).toHaveLength(DEFAULT_IW_EXPORTED_STATIC_COLUMNS.length);
    staticColumns.forEach((column, index) => {
      expect(column.text()).toEqual(columnMap[DEFAULT_IW_EXPORTED_STATIC_COLUMNS[index]].name);
    });

    const tableBodies = app.find(TableBody);
    expect(tableBodies).toHaveLength(2);

    const fixedRows = tableBodies.at(0).find(TableRow);
    expect(fixedRows).toHaveLength(1);

    const fixedRowColumns = fixedRows.first().find(TableCell);
    expect(fixedRowColumns).toHaveLength(DEFAULT_IW_EXPORTED_FIXED_COLUMNS.length);
    fixedRowColumns.forEach((cell, index) => {
      expect(cell.find(columnMap[DEFAULT_IW_EXPORTED_FIXED_COLUMNS[index]].formatter)).toHaveLength(1);
      // Technically, I should be manually checking values here but fsck with this schiit
    });

    const staticRows = tableBodies.at(1).find(TableRow);
    expect(staticRows).toHaveLength(1);

    const statisRowColumns = staticRows.first().find(TableCell);
    expect(statisRowColumns).toHaveLength(DEFAULT_IW_EXPORTED_STATIC_COLUMNS.length);
    statisRowColumns.forEach((cell, index) => {
      expect(cell.find(columnMap[DEFAULT_IW_EXPORTED_STATIC_COLUMNS[index]].formatter)).toHaveLength(1);
      // See the same comment for fixed columns
    });
  });
});
