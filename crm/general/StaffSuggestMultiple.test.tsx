import React from 'react';
import { render, screen, getByLabelText, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { Grid } from '@crm/components/dist/Attribute2/components/Grid';
import { StatefulStaffSuggestMultiple } from './StatefulStaffSuggestMultiple';
import { User } from '../types/User';
import { genUser } from '../utils/genUser';

describe('Attribute2/StaffSuggestMultiple', () => {
  it('renders label', () => {
    render(
      <Grid>
        <StatefulStaffSuggestMultiple label="label" defaultValue={[]} />
      </Grid>,
    );

    const readingArea = screen.getByLabelText('reading area');
    const label = getByLabelText(readingArea, 'label');
    expect(label).toBeVisible();

    cleanup();
  });

  it('renders value', () => {
    render(
      <Grid>
        <StatefulStaffSuggestMultiple
          label="label"
          defaultValue={[
            genUser({ id: 1, name: 'value', login: 'test', group: { name: 'test group' } }),
          ]}
        />
      </Grid>,
    );

    const readingArea = screen.getByLabelText('reading area');
    const value = getByLabelText(readingArea, 'value');
    expect(value).toBeVisible();

    cleanup();
  });

  describe('after click', () => {
    beforeEach(() => {
      const users: User[] = [
        genUser({ id: 1, name: 'test user 1', login: 'test1', group: { name: 'group 1' } }),
        genUser({ id: 2, name: 'test user 2', login: 'test2', group: { name: 'group 2' } }),
      ];
      const loadUsers = () => users;
      render(
        <Grid>
          <StatefulStaffSuggestMultiple label="label" onLoad={loadUsers} defaultValue={[]} />
        </Grid>,
      );

      const readingArea = screen.getByLabelText('reading area');
      const label = getByLabelText(readingArea, 'label');
      userEvent.click(label);
    });

    afterEach(() => {
      cleanup();
    });

    it('hides reading area', () => {
      const readingArea = screen.getByLabelText('reading area');

      expect(readingArea).not.toBeVisible();
    });

    it('shows users', () => {
      const testUser2 = screen.getByText('test user 2');
      expect(testUser2).toBeVisible();
    });

    it('finishes editing on outside click', () => {
      userEvent.click(document.body);

      const testUser2 = screen.getByText('test user 2');
      expect(testUser2).not.toBeVisible();
    });
  });

  describe('after click on user', () => {
    beforeEach(() => {
      const users: User[] = [
        genUser({ id: 1, name: 'test user 1', login: 'test1', group: { name: 'group 1' } }),
        genUser({ id: 2, name: 'test user 2', login: 'test2', group: { name: 'group 2' } }),
      ];
      const loadUsers = () => users;
      render(
        <Grid>
          <StatefulStaffSuggestMultiple label="label" onLoad={loadUsers} defaultValue={[]} />
        </Grid>,
      );

      const readingArea = screen.getByLabelText('reading area');
      const label = getByLabelText(readingArea, 'label');
      userEvent.click(label);

      const textbox = screen.getByRole('textbox');
      userEvent.type(textbox, 'test');

      userEvent.click(screen.getByText('test user 2'));
    });

    afterEach(() => {
      cleanup();
    });

    it('renders new value', () => {
      const readingArea = screen.getByLabelText('reading area');
      const value = getByLabelText(readingArea, 'value');
      expect(value).toHaveTextContent('test user 2');
    });

    it('clears textbox', () => {
      const textbox = screen.getByRole('textbox');
      expect(textbox).toBeEmptyDOMElement();
    });
  });

  describe('keyboard navigation', () => {
    beforeAll(() => {
      const load = () => [
        genUser({ id: 1, name: 'test user 1', login: 'test1', group: { name: 'group 1' } }),
        genUser({ id: 2, name: 'test user 2', login: 'test2', group: { name: 'group 2' } }),
        genUser({ id: 3, name: 'test user 3', login: 'test3', group: { name: 'group 3' } }),
        genUser({ id: 4, name: 'test user 4', login: 'test4', group: { name: 'group 4' } }),
      ];

      render(
        <Grid>
          <StatefulStaffSuggestMultiple label="label" onLoad={load} defaultValue={[]} />
          <StatefulStaffSuggestMultiple label="label" onLoad={load} defaultValue={[]} />
          <StatefulStaffSuggestMultiple label="label" onLoad={load} defaultValue={[]} />
        </Grid>,
      );

      userEvent.tab();
      userEvent.keyboard('{enter}');
    });

    it('focuses self with tab', () => {
      const readingAreas = screen.getAllByLabelText('reading area');
      expect(readingAreas[0]).toHaveFocus();

      userEvent.tab();
      expect(readingAreas[1]).toHaveFocus();

      userEvent.tab();
      expect(readingAreas[2]).toHaveFocus();

      userEvent.tab();
      expect(readingAreas[0]).toHaveFocus();

      userEvent.tab({ shift: true });
      expect(readingAreas[2]).toHaveFocus();

      userEvent.tab({ shift: true });
      expect(readingAreas[1]).toHaveFocus();

      userEvent.tab({ shift: true });
      expect(readingAreas[0]).toHaveFocus();
    });

    it('shows users after enter', () => {
      userEvent.keyboard('{enter}');

      const readingAreas = screen.getAllByLabelText('reading area');

      expect(readingAreas[0]).not.toBeVisible();
      expect(screen.getByText('test user 1')).toBeInTheDocument();
    });

    it('changes with multiple users', async () => {
      userEvent.keyboard('{arrowdown}{enter}');
      userEvent.keyboard('{arrowdown}{enter}');
      userEvent.keyboard('{arrowdown}{enter}');
      userEvent.keyboard('{esc}');

      const readingAreas = screen.getAllByLabelText('reading area');
      expect(readingAreas[0]).toBeVisible();

      expect(readingAreas[0]).toBeVisible();
      const value = getByLabelText(readingAreas[0], 'value');
      expect(value).toHaveTextContent('test user 2');
      expect(value).toHaveTextContent('test user 3');
      expect(value).toHaveTextContent('test user 4');
    });
  });
});
