import { Client } from 'pg';

import { GOOD_COVERAGE_PATH, LOW_COVERAGE_PATH } from '../test/data';
import { writeCurrentCoverage, compareCurrentCoverage } from './totalCoverage';
import { mapToDBItem } from '../storage';
import { CoverageReport } from '../types';
import GOOD_COVERAGE from '../test/files/good-coverage-report.json';

const coverageOptions = {
  projectName: 'ir-ui',
  envBranchName: 'master',
  reportPath: GOOD_COVERAGE_PATH,
};

const connectionOptions = {
  password: '123',
};

describe('totalCoverage checker', () => {
  const clientConnectMock = jest.fn();
  const clientEndMock = jest.fn();
  const clientQuery = jest.fn(async () => ({
    rows: [mapToDBItem((GOOD_COVERAGE.total as unknown) as CoverageReport, coverageOptions)],
  }));

  const checkCallDb = () => {
    expect(clientConnectMock).toBeCalledTimes(1);
    expect(clientEndMock).toBeCalledTimes(1);
    expect(clientQuery).toBeCalledTimes(1);
  };

  beforeAll(() => {
    Client.prototype.connect = clientConnectMock;
    Client.prototype.query = clientQuery as any;
    Client.prototype.end = clientEndMock as any;
  });

  beforeEach(() => {
    clientConnectMock.mockClear();
    clientEndMock.mockClear();
    clientQuery.mockClear();
  });

  afterAll(() => {
    Client.prototype.connect = clientConnectMock;
    Client.prototype.query = clientQuery as any;
    Client.prototype.end = clientEndMock as any;
  });

  test('writeCurrentCoverage', async () => {
    const result = await writeCurrentCoverage(connectionOptions, coverageOptions);

    expect(result).toBe(undefined);

    checkCallDb();
  });

  test('compareCurrentCoverage good coverage', async () => {
    const result = await compareCurrentCoverage(connectionOptions, coverageOptions);

    expect(result).toBe(undefined);

    checkCallDb();
  });

  test('compareCurrentCoverage low coverage', async () => {
    const result = await compareCurrentCoverage(connectionOptions, {
      ...coverageOptions,
      reportPath: LOW_COVERAGE_PATH,
    });

    expect(result?.length).toBe(3);

    checkCallDb();
  });

  test('compareCurrentCoverage without actual coverage', async () => {
    // на заппрос актуального покрытия возвращаем пустоту
    Client.prototype.query = async () => ({ rows: [] } as any);

    const result = await compareCurrentCoverage(connectionOptions, {
      ...coverageOptions,
      reportPath: LOW_COVERAGE_PATH,
    });

    expect(result).toBe(undefined);
  });
});
