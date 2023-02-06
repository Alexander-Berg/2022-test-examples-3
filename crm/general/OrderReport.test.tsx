import React from 'react';
import { render, waitFor } from '@testing-library/react';
import { OrderReport } from './OrderReport';
import { Report } from './OrderReport.types';

describe('OrderReport', () => {
  beforeEach(() => {
    window.open = jest.fn();
  });

  it('renders props.reports as items', async () => {
    const handleOrder = () => Promise.resolve('');
    const reports: Report[] = [
      {
        id: 0,
        name: 'Report 0',
        url: '0',
      },
      {
        id: 1,
        name: 'Report 1',
        url: '1',
      },
    ];

    // eslint-disable-next-line
    const { getByRole, getByText } = render(<OrderReport reports={reports} onOrder={handleOrder} />);

    getByRole('button').click();

    await waitFor(() => {
      expect(getByText('Report 0')).toBeInTheDocument();
      expect(getByText('Report 1')).toBeInTheDocument();
    });
  });

  describe('when clicked on report', () => {
    let report: HTMLElement;
    let handleOrder = jest.fn(() => Promise.resolve('test-url'));
    beforeEach(async () => {
      const reports: Report[] = [
        {
          id: 0,
          name: 'Report 0',
          url: '0',
        },
      ];

      // eslint-disable-next-line
      const { getByRole, getByText } = render(<OrderReport reports={reports} onOrder={handleOrder} />);
      getByRole('button').click();
      report = await waitFor(() => {
        return getByText('Report 0');
      });
    });

    it('calls props.onOrder', async () => {
      report.click();

      await waitFor(() => {
        expect(handleOrder).toBeCalledWith('0');
      });
    });

    it('uses props.onOrder returned url', async () => {
      report.click();

      await waitFor(() => {
        expect((window.open as jest.Mock).mock.calls[0][0]).toBe('test-url');
      });
    });
  });
});
