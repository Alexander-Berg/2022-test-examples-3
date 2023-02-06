import React from 'react';
import { render, screen, cleanup, waitFor } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { UserCell } from './UserCell';
import { mockedFakeProps, mockedProps } from './__mocks__';

describe('UserCell', () => {
  afterEach(() => {
    cleanup();
  });
  it('renders', () => {
    render(<UserCell {...mockedProps} />);
    expect(screen.queryByText('User Name')).toBeInTheDocument();
  });
  describe('when hovering', () => {
    it('renders the staff card', () => {
      render(<UserCell {...mockedProps} />);
      userEvent.hover(screen.getByText('User Name'));
      waitFor(() => {
        expect(screen.queryByText('Группа разработки CRM')).toBeInTheDocument();
      });
      waitFor(() => {
        expect(screen.queryByText('userlogin@yandex-team.ru')).toBeInTheDocument();
      });
    });
  });

  describe('user.login', () => {
    describe('when defined', () => {
      it('renders link', () => {
        render(<UserCell {...mockedProps} />);

        expect(screen.queryByTestId('user-link')).toBeInTheDocument();
      });

      cleanup();
    });
    describe('when not defined', () => {
      it('does not render link', () => {
        render(<UserCell {...mockedFakeProps} />);

        expect(screen.queryByTestId('user-link')).not.toBeInTheDocument();
      });

      cleanup();
    });
  });
});
