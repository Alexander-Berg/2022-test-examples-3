import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { SoftPhoneInfo } from './SoftPhoneInfo';
import { Provider } from '../services/TerminalTypeService';
import { TerminalTypeServiceStub } from '../services/TerminalTypeService/TerminalTypeService.stub';

describe('SoftPhoneInfo', () => {
  describe('when is loading', () => {
    it('renders spin', () => {
      const service = new TerminalTypeServiceStub();
      service.isSoftPhoneInfoLoading = true;

      const { container } = render(
        <Provider value={service}>
          <SoftPhoneInfo />
        </Provider>,
      );

      expect(container.getElementsByClassName('crm-spinner').length).toBe(1);
    });
  });

  describe("when isn't loading", () => {
    it('renders spin', () => {
      const service = new TerminalTypeServiceStub();

      const { container } = render(
        <Provider value={service}>
          <SoftPhoneInfo />
        </Provider>,
      );

      expect(container.getElementsByClassName('crm-spinner').length).toBe(0);
    });
  });

  describe('when softPhoneInfo is defined', () => {
    it('renders softPhone info', () => {
      const service = new TerminalTypeServiceStub();

      render(
        <Provider value={service}>
          <SoftPhoneInfo />
        </Provider>,
      );

      expect(screen.getByText('password')).toBeInTheDocument();
      expect(screen.getByText('111')).toBeInTheDocument();
      expect(screen.getByText('222')).toBeInTheDocument();
      expect(screen.getByText('host')).toBeInTheDocument();
      expect(screen.getByText('login')).toBeInTheDocument();
    });
  });

  describe('when reset password', () => {
    it('calls onClick', () => {
      const service = new TerminalTypeServiceStub();

      render(
        <Provider value={service}>
          <SoftPhoneInfo />
        </Provider>,
      );

      fireEvent.click(screen.getByText('Поменять пароль'));
      expect(screen.getByText('newpassword')).toBeInTheDocument();
    });
  });
});
