import React from 'react';
import { render, screen, getByLabelText, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { Grid } from '@crm/components/dist/Attribute2/components/Grid';
import { StatefulDate } from './StatefulDate';
import { prettyValue } from './Reading/Reading.utils';

const date = new Date();
date.setMonth(11);
date.setDate(1);
date.setFullYear(2021);

describe('Attribute2/Date', () => {
  it('renders label', () => {
    render(
      <Grid>
        <StatefulDate label="label" defaultValue={null} />
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
        <StatefulDate label="label" defaultValue={date.toISOString()} />
      </Grid>,
    );

    const readingArea = screen.getByLabelText('reading area');
    const value = getByLabelText(readingArea, 'value');
    expect(value).toBeVisible();
    expect(value).toHaveTextContent(prettyValue(date.toISOString())!);

    cleanup();
  });

  describe('after click', () => {
    beforeEach(() => {
      render(
        <Grid>
          <StatefulDate label="label" defaultValue={date.toISOString()} />
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

    it('shows calendar', () => {
      expect(screen.getByText('декабрь')).toBeVisible();
      expect(screen.getByText('2021')).toBeVisible();
    });

    it('finishes editing on outside click', () => {
      userEvent.click(document.body);

      expect(screen.getByText('декабрь')).not.toBeVisible();
      expect(screen.getByText('2021')).not.toBeVisible();
    });
  });

  describe('after click on date', () => {
    beforeEach(() => {
      render(
        <Grid>
          <StatefulDate label="label" defaultValue={date.toISOString()} />
        </Grid>,
      );

      const readingArea = screen.getByLabelText('reading area');
      const label = getByLabelText(readingArea, 'label');
      userEvent.click(label);

      userEvent.click(screen.getByText('10'));
    });

    afterEach(() => {
      cleanup();
    });

    it('renders new value', () => {
      const choosenDate = new Date(date);
      choosenDate.setDate(10);
      const readingArea = screen.getByLabelText('reading area');
      const value = getByLabelText(readingArea, 'value');
      expect(value).toHaveTextContent(prettyValue(choosenDate.toISOString())!);
    });
  });

  describe('keyboard navigation', () => {
    beforeAll(() => {
      render(
        <Grid>
          <StatefulDate label="label" defaultValue={date.toISOString()} />
          <StatefulDate label="label" defaultValue={date.toISOString()} />
          <StatefulDate label="label" defaultValue={date.toISOString()} />
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

    it('shows calendar after enter', () => {
      userEvent.keyboard('{enter}');

      const readingAreas = screen.getAllByLabelText('reading area');

      expect(readingAreas[0]).not.toBeVisible();
      expect(screen.getByText('декабрь')).toBeInTheDocument();
      expect(screen.getByText('2021')).toBeInTheDocument();
    });

    it('hides calendar after escape', () => {
      const readingAreas = screen.getAllByLabelText('reading area');

      userEvent.keyboard('{esc}');
      expect(readingAreas[0]).toBeVisible();
    });
  });
});
