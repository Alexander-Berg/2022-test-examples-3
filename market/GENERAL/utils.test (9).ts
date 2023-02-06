import { filterReports } from './utils';
import { otraceResponse } from 'src/test/mockData/otraceResponse';
import { AdvancedOTraceResponse } from './components';
import { FailedReport } from 'src/entities/otraceReport/types';

describe('Report/utils', () => {
  it('filterReports', () => {
    const loadedReports: AdvancedOTraceResponse[] = [otraceResponse as unknown as AdvancedOTraceResponse];
    const failedReports: FailedReport[] = [
      {
        feedId: 123,
        response: 'text',
      },
    ];

    expect(filterReports([...loadedReports, ...failedReports], { hideFailedReports: false })).toEqual([
      ...loadedReports,
      ...failedReports,
    ]);

    expect(filterReports([...loadedReports, ...failedReports], { hideFailedReports: true })).toEqual(loadedReports);
  });
});
