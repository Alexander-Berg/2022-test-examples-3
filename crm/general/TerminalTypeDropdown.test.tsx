import React from 'react';
import { render, fireEvent, screen } from '@testing-library/react';
import { TerminalTypeDropdown } from './TerminalTypeDropdown';
import { Provider } from '../services/TerminalTypeService';
import { TerminalTypeServiceStub } from '../services/TerminalTypeService/TerminalTypeService.stub';
import { TerminalTypeNames } from './TerminalTypeDropdown.constants';

describe('TerminalTypeDropdown', () => {
  describe('when select terminal type', () => {
    describe('when is loading', () => {
      it('renders spin and disables select', () => {
        const service = new TerminalTypeServiceStub();

        const { container } = render(
          <Provider value={service}>
            <TerminalTypeDropdown />
          </Provider>,
        );

        const select = screen.getByRole('listbox');

        fireEvent.click(select);
        fireEvent.click(screen.getByText(TerminalTypeNames.SOFTPHONE));

        expect(select).toBeDisabled();
        expect(container.getElementsByClassName('Spin2_progress').length).toBe(1);
      });
    });

    describe("when isn't loading", () => {
      it("doesn't render spin and disable select", () => {
        const service = new TerminalTypeServiceStub();

        const { container } = render(
          <Provider value={service}>
            <TerminalTypeDropdown />
          </Provider>,
        );

        expect(screen.getByRole('listbox')).not.toBeDisabled();
        expect(container.getElementsByClassName('Spin2_progress').length).toBe(0);
      });
    });
  });
});
