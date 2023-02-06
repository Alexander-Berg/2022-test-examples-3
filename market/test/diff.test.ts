import path from 'path';

import { calcDiff, cutFilePath } from '../diff';
import { fromJsonFile } from '../utils';

const currentReport = fromJsonFile(path.join(__dirname, './files/low-coverage-report.json'));
const baseReport = fromJsonFile(path.join(__dirname, './files/good-coverage-report.json'));
const expectedDiff = fromJsonFile(path.join(__dirname, './files/report-dif.json'));

describe('json report', () => {
  test('getFilePath', async () => {
    const filePath = cutFilePath(
      '/place/sandbox-data/tasks/0/5/1336536150/mounted_arcadia/market/mbo/content-mapping/content-mapping-app/src/frontend/src/AppContent.tsx',
      'frontend'
    );

    expect(filePath).toBe('frontend/src/AppContent.tsx');
  });
  test('diff', async () => {
    const diff = calcDiff(baseReport, currentReport, 'frontend');
    expect(diff).toEqual(expectedDiff);
  });

  test('diff отрабатывает без ошибок когда в новом отчете появляется новый юнит', async () => {
    const newItem = { 'new-item': currentReport.total };
    const diff = calcDiff(baseReport, { ...currentReport, ...newItem }, 'frontend');
    expect(diff).toEqual({ ...expectedDiff, ...newItem });
  });
});
