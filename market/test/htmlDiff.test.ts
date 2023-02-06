import path from 'path';
import fs from 'fs';

import { getHtmlDiffTable, writeHtmlDiff } from '../htmlDiff';
import { diffHtmlTable } from './data';
import expectedDiff from '../../test/files/report-dif.json';

describe('html diff', () => {
  test('diff html template', async () => {
    const diffHtmlStr = getHtmlDiffTable(expectedDiff);
    // eslint-disable-next-line no-control-regex
    expect(diffHtmlStr.replace(new RegExp('(\n| )', 'g'), '')).toBe(diffHtmlTable);
  });

  test('diff inject', async () => {
    const diffHtml = getHtmlDiffTable(expectedDiff);
    const htmlPath = path.join(__dirname, './report.html');

    // запоминаем файл, что бы в конце теста очистить его
    const clearFile = fs.readFileSync(htmlPath, 'utf8');

    writeHtmlDiff(diffHtml, htmlPath);
    // чекаем что диф добавился в html
    expect(fs.readFileSync(htmlPath, 'utf8')).toContain(diffHtml);

    // очищаем
    fs.writeFileSync(htmlPath, clearFile);
  });
});
