import React from 'react';
import { withRouter } from 'react-router';
import { rest, ResponseResolver, MockedRequest, restContext } from 'msw';

import { DashboardPage as Page } from '.';
import { ConnectionStatistic, TmsJobStatistic, DumpStatistic } from './store';

import { render, server, waitFor, screen } from '@/test-utils';

const DashboardPage = withRouter(Page);

const getMockConnectionStatisticsApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: ConnectionStatistic[] = [{ data: ['#1 mbo-cms-api', '20'], status: 'success' }];
  return res(ctx.status(200), ctx.body(data));
};

const getMockTmsJobsStatisticsApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: TmsJobStatistic[] = [{ data: ['#2 20200924_2120', '3 min', 'sas2-3723-sas-market'], status: 'success' }];
  return res(ctx.status(200), ctx.body(data));
};

const getMockDumpStatusApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const data: DumpStatistic[] = [{ data: ['#3 20200924_2120', '3 min', 'sas2-3723-sas-market'], status: 'success' }];
  return res(ctx.status(200), ctx.body(data));
};

describe('<DashboardPage />', () => {
  beforeEach(() => {
    server.use(
      rest.get('/api/v1/dashboard/connection-statistics', getMockConnectionStatisticsApiResponse),
      rest.get('/api/v1/dashboard/tms-jobs-statistics', getMockTmsJobsStatisticsApiResponse),
      rest.get('/api/v1/dashboard/dump-status', getMockDumpStatusApiResponse)
    );
  });

  it('should be rendered without errors', () => {
    expect(() => {
      render(<DashboardPage />);
    }).not.toThrow();
  });

  it('should be render a connections statistics', async () => {
    render(<DashboardPage />);

    await waitFor(() => screen.getByTitle('#1 mbo-cms-api'));

    expect(screen.getByTitle('#1 mbo-cms-api')).toBeInTheDocument();
  });

  it('should be render a tsm job statistics', async () => {
    render(<DashboardPage />);

    await waitFor(() => screen.getByTitle('#2 20200924_2120'));

    expect(screen.getByTitle('#2 20200924_2120')).toBeInTheDocument();
  });

  it('should be render a dump statistics', async () => {
    render(<DashboardPage />);

    await waitFor(() => screen.getByTitle('#3 20200924_2120'));

    expect(screen.getByTitle('#3 20200924_2120')).toBeInTheDocument();
  });
});
