import React from 'react';
import Bluebird from 'bluebird';
import { render, fireEvent, waitFor } from '@testing-library/react';
import CreateLeadButton from './CreateLeadButton';
import { saveClient } from '../withCreateApi/api';

jest.mock('../withCreateApi/api', () => ({
  saveClient: jest.fn(),
}));

(saveClient as jest.Mock).mockImplementation(() => {
  return Bluebird.resolve({
    account: {
      id: 132,
    },
  });
});

describe('CreateLeadButton', () => {
  describe('props.disabled', () => {
    let handleCreate: () => void;
    beforeEach(() => {
      handleCreate = jest.fn();
    });

    describe('is truthy', () => {
      it('does not call props.onCreate', () => {
        const { getByRole } = render(<CreateLeadButton onCreate={handleCreate} disabled />);

        fireEvent.click(getByRole('button'));

        expect(handleCreate).not.toBeCalled();
      });
    });

    describe('is falsy', () => {
      it('calls props.onCreate', async () => {
        const { getByRole } = render(<CreateLeadButton onCreate={handleCreate} />);

        fireEvent.click(getByRole('button'));

        await waitFor(() => {
          expect(handleCreate).toBeCalled();
        });
      });
    });
  });
});
