import { combine, createStore } from '@reatom/core';

import { OperatorStatistics, StatisticsResponse } from 'src/java/definitions';
import { StatisticsActions } from 'src/pages/Statistics/store/actions';
import { processAggregatedStatistics } from 'src/pages/Statistics/store/atoms/statisticsAtoms.utils';
import { StatisticsAtom } from 'src/pages/Statistics/store/atoms';

const testMboUser1 = {
  email: '1',
  fullname: '1',
  login: '1',
  pureLogin: '1',
  staffEmail: '1',
  staffLogin: '1',
  uid: 1,
  yandexEmail: '1',
};
const testMboUser2 = {
  ...testMboUser1,
  uid: 2,
};

const testWorkload = {
  currentWorkload: 1,
  maxWorkload: 1,
};

const testCategoryWorkload = {
  offersCountFromAliasMaker: 1,
  offersCountFromMarkupWorker: 1,
};

const testCategoryStat = {
  categoryCompleteness: 1,
  categoryName: '1',
  categoryRank: 1,
  categoryWorkload: 1,
  createdMappingCount: 1,
  createdModelCount: 1,
  createdSkuCount: 1,
  processingOffersCount: 1,
  trashMappingsCount: 1,
};

const testStat1: OperatorStatistics[] = [
  {
    categoryId: -1,
    inputManager: undefined,
    operator: undefined,
    categoryStatistics: testCategoryStat,
    globalCompleteness: -1,
    globalRank: -1,
    operatorWorkload: testWorkload,
    superOperatorWorkload: testWorkload,
    categoryWorkload: testCategoryWorkload,
  },
  {
    categoryId: -1,
    inputManager: testMboUser1,
    operator: undefined,
    categoryStatistics: testCategoryStat,
    globalCompleteness: -1,
    globalRank: -1,
    operatorWorkload: testWorkload,
    superOperatorWorkload: testWorkload,
    categoryWorkload: testCategoryWorkload,
  },
  {
    categoryId: -1,
    inputManager: testMboUser1,
    operator: testMboUser1,
    categoryStatistics: testCategoryStat,
    globalCompleteness: -1,
    globalRank: -1,
    operatorWorkload: testWorkload,
    superOperatorWorkload: testWorkload,
    categoryWorkload: testCategoryWorkload,
  },
  {
    categoryId: -1,
    inputManager: testMboUser1,
    operator: testMboUser2,
    categoryStatistics: testCategoryStat,
    globalCompleteness: -1,
    globalRank: -1,
    operatorWorkload: testWorkload,
    superOperatorWorkload: testWorkload,
    categoryWorkload: testCategoryWorkload,
  },
];

const testResponse: StatisticsResponse = {
  operatorStatisticsList: testStat1,
  serviceAvailability: {
    aliasMakerAvailability: true,
    markupWorkerAvailability: true,
  },
};

const testOrganizedStat1 = [
  {
    id: 1,
    total: {
      categoryId: -1,
      inputManager: testMboUser1,
      operator: undefined,
      categoryStatistics: testCategoryStat,
      globalCompleteness: -1,
      globalRank: -1,
      operatorWorkload: testWorkload,
      superOperatorWorkload: testWorkload,
      categoryWorkload: testCategoryWorkload,
    },
    operators: [
      {
        id: 1,
        total: {
          categoryId: -1,
          inputManager: testMboUser1,
          operator: testMboUser1,
          categoryStatistics: testCategoryStat,
          globalCompleteness: -1,
          globalRank: -1,
          operatorWorkload: testWorkload,
          superOperatorWorkload: testWorkload,
          categoryWorkload: testCategoryWorkload,
        },
      },
      {
        id: 2,
        total: {
          categoryId: -1,
          inputManager: testMboUser1,
          operator: testMboUser2,
          categoryStatistics: testCategoryStat,
          globalCompleteness: -1,
          globalRank: -1,
          operatorWorkload: testWorkload,
          superOperatorWorkload: testWorkload,
          categoryWorkload: testCategoryWorkload,
        },
      },
    ],
  },
  {
    id: -1,
    total: {
      categoryId: -1,
      inputManager: undefined,
      operator: undefined,
      categoryStatistics: testCategoryStat,
      globalCompleteness: -1,
      globalRank: -1,
      operatorWorkload: testWorkload,
      superOperatorWorkload: testWorkload,
      categoryWorkload: testCategoryWorkload,
    },
    operators: undefined,
  },
];

describe('Statistics Page reducer', () => {
  it('organize aggregated data', () => {
    const store = createStore(combine([StatisticsAtom]));
    store.dispatch(
      StatisticsActions.loadStatistics.done(processAggregatedStatistics(testResponse.operatorStatisticsList))
    );
    expect(store.getState(StatisticsAtom)).toEqual(testOrganizedStat1);
  });
});
