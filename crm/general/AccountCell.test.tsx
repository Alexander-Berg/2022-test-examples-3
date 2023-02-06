import React from 'react';
import { render, screen, cleanup, waitFor } from '@testing-library/react/pure';
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mocked } from 'ts-jest/utils';
import promise from 'bluebird';
import { AccountCell } from './AccountCell';
import { mockedInfo, mockedProps } from './__mocks__';
import { getAccountShortInfo } from './getAccountShortInfo';

jest.mock('./getAccountShortInfo', () => ({
  getAccountShortInfo: jest.fn(),
}));

describe('AccountCell', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<AccountCell {...mockedProps} />);
    expect(screen.queryByText('AccountName')).toBeInTheDocument();
    expect(screen.queryByText('Клиент')).toBeInTheDocument();
  });

  it('generates correct links', () => {
    const { container } = render(<AccountCell {...mockedProps} />);
    expect(container.querySelector(`a[href$='account/332633']`)).toBeInTheDocument();
  });

  it('shows account tooltip', async () => {
    mocked(getAccountShortInfo).mockReturnValueOnce(promise.resolve(mockedInfo));

    render(<AccountCell {...mockedProps} />);

    act(() => {
      userEvent.hover(screen.getByTestId('grid-cell-account-name'));
    });

    await waitFor(() =>
      expect(screen.getByTestId('grid-cell-account-tooltip')).toBeInTheDocument(),
    );

    expect(screen.getByTestId('tier-label')).toBeInTheDocument();
    expect(screen.getByTestId('contragent-label')).toBeInTheDocument();
    expect(screen.getByTestId('managers-label')).toBeInTheDocument();
    expect(screen.getByTestId('agency-label')).toBeInTheDocument();
  });
});
