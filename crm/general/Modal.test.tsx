import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider, Store } from '../../State';
import { Modal } from './Modal';
import { modalTestId } from './ModalContent';
import { BoolState } from '../../State/defaultStores/BoolState';

jest.mock('../TopBar', () => ({
  TopBar: () => null,
}));

jest.mock('../Categorization', () => ({
  Categorization: () => null,
}));

describe('components/Modal', () => {
  beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
  });

  afterAll(() => {
    jest.clearAllMocks();
  });

  describe('state.openess.state', () => {
    describe('is true', () => {
      it('renders modal', () => {
        const store = ({
          openess: new BoolState(true),
          fullness: new BoolState(true),
          root: [],
          expanded: [],
          highlighted: [],
          selected: [],
          close: jest.fn(),
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <Modal />
          </Provider>,
        );

        const modal = screen.getByTestId(modalTestId);

        expect(modal).toBeInTheDocument();
      });
    });

    describe('is false', () => {
      it(`doesn't render modal`, () => {
        const store = ({
          openess: new BoolState(false),
          fullness: new BoolState(true),
          close: jest.fn(),
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <Modal />
          </Provider>,
        );

        const modal = screen.queryByTestId(modalTestId);

        expect(modal).not.toBeInTheDocument();
      });
    });
  });

  describe('state.previewComponent', () => {
    it('renders preview component', () => {
      const Preview = () => <div>Preview</div>;
      const store = ({
        openess: new BoolState(true),
        fullness: new BoolState(true),
        root: [],
        expanded: [],
        highlighted: [],
        selected: [],
        previewComponent: Preview,
        close: jest.fn(),
      } as unknown) as Store;

      render(
        <Provider store={store}>
          <Modal />
        </Provider>,
      );

      const preview = screen.getByText('Preview');

      expect(preview).toBeInTheDocument();
    });
  });
});
