import React from 'react';
import { render, screen, cleanup } from '@testing-library/react/pure';
import cloneDeep from 'lodash/cloneDeep';
import { CustomViewCell } from './CustomViewCell';
import { TicketShortInfoProps } from './Views/TicketShortInfo';
import { MESSAGE } from '../UnknownCell/Unknown.constants';

const mockedProps: TicketShortInfoProps = {
  options: { module: 'Cases' },
  cell: {
    id: '1',
    type: 'CustomView',
    data: {
      viewName: 'TicketShortInfo',
      viewData: {
        id: '12345',
        title: 'TestTitle',
        accountId: '007',
        accountName: 'TestAccountName',
      },
    },
  },
};

describe('CustomViewCell', () => {
  afterEach(() => {
    cleanup();
  });
  describe('TicketShortInfo', () => {
    it('renders', () => {
      render(<CustomViewCell {...mockedProps} />);
      expect(screen.queryByText('TestTitle')).toBeInTheDocument();
      expect(screen.queryByText('TestAccountName')).toBeInTheDocument();
      expect(screen.queryByText('12345')).toBeInTheDocument();
    });
    it('generates correct links', () => {
      const { container } = render(<CustomViewCell {...mockedProps} />);
      expect(container.querySelector(`a[href$='007']`)).toBeInTheDocument();
      expect(container.querySelector(`a[href$='cases/12345']`)).toBeInTheDocument();
    });
    describe('if got unknown ViewName', () => {
      it('renders unknown cell', () => {
        const wrongProps = cloneDeep(mockedProps);
        wrongProps.cell.data.viewName = 'UnknownViewName';
        render(<CustomViewCell {...wrongProps} />);
        expect(screen.queryByText(MESSAGE)).toBeInTheDocument();
      });
    });
  });
});
