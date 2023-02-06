import { render, waitFor, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { TestBed } from 'components/TestBed';
import { createOpportunitiesFields } from 'modules/issues/components/Timeline/OpportunitiesContainer';
import { NewMailForm } from '../NewMailForm.bundle/base';

jest.mock('components/RichHtmlEditor');

const props = {
  froms: [],
  signatures: [],
  templates: [],
  initialValues: {
    subject: 'Subject',
    opportunities: [
      { id: '1', name: 'Opportunity 1' },
      { id: '2', name: 'Opportunity 2' },
    ],
  },
};

const account = {
  id: 1,
  info: {
    login: '1',
  },
};

jest.useFakeTimers();

describe('NewMailForm', () => {
  describe('opportunities', () => {
    let form;
    beforeEach(async () => {
      form = render(
        <TestBed>
          <NewMailForm
            {...props}
            isPreventUnload
            hasAutoSave
            OpportunitiesInput={createOpportunitiesFields({ account })}
          />
        </TestBed>,
      );
    });

    afterEach(() => {
      cleanup();
    });

    it('renders opportunities', async () => {
      await waitFor(() => {
        expect(form.queryByText('Сделки')).toBeInTheDocument();
        expect(form.queryByText('Opportunity 1')).toBeInTheDocument();
      });
    });

    it('removes opportunities on noLinkWithOpportunity change', async () => {
      const checkbox = await waitFor(() => form.getByText('Письмо не относится к сделке'));
      expect(form.queryByText('Opportunity 1')).toBeInTheDocument();

      await waitFor(() => {
        userEvent.click(checkbox);
        expect(form.queryByText('Сделки')).not.toBeInTheDocument();
      });

      await waitFor(() => {
        userEvent.click(checkbox);
        expect(form.queryByText('Сделки')).toBeInTheDocument();
        expect(form.queryByText('Opportunity 1')).not.toBeInTheDocument();
      });
    });
  });
});
