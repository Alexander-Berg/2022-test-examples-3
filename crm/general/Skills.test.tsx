import React from 'react';
import { render, screen, getByLabelText, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { Grid } from '@crm/components/dist/Attribute2/components/Grid';
import { StatefulSkills } from './StatefulSkills';
import { Skill } from '../types/Skill';

describe('Attribute2/Skills', () => {
  it('renders label', () => {
    render(
      <Grid>
        <StatefulSkills label="label" defaultValue={[]} />
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
        <StatefulSkills label="label" defaultValue={[{ id: 1, text: 'value', value: 3 }]} />
      </Grid>,
    );

    const readingArea = screen.getByLabelText('reading area');
    const value = getByLabelText(readingArea, 'value');
    expect(value).toBeVisible();
    expect(value).toHaveTextContent('value 3');

    cleanup();
  });

  describe('after click', () => {
    beforeEach(() => {
      const skills: Skill[] = [
        { id: 1, text: 'test skill 1' },
        { id: 2, text: 'test skill 2' },
      ];
      const loadSkills = () => skills;
      render(
        <Grid>
          <StatefulSkills label="label" onLoad={loadSkills} defaultValue={[]} />
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

    it('shows skills', () => {
      const testSkill2 = screen.getByText('test skill 2');
      expect(testSkill2).toBeVisible();
    });

    it('finishes editing on outside click', () => {
      userEvent.click(document.body);

      const testSkill2 = screen.getByText('test skill 2');
      expect(testSkill2).not.toBeVisible();
    });
  });

  describe('after click on skill in popup', () => {
    beforeEach(() => {
      const skills: Skill[] = [
        { id: 1, text: 'test skill 1' },
        { id: 2, text: 'test skill 2' },
      ];
      const loadSkills = () => skills;
      render(
        <Grid>
          <StatefulSkills label="label" onLoad={loadSkills} defaultValue={[]} />
        </Grid>,
      );

      const readingArea = screen.getByLabelText('reading area');
      const label = getByLabelText(readingArea, 'label');
      userEvent.click(label);

      userEvent.click(screen.getByText('test skill 2'));
    });

    afterEach(() => {
      cleanup();
    });

    it('renders new value', () => {
      userEvent.click(screen.getByText('2'));

      const readingArea = screen.getByLabelText('reading area');
      const value = getByLabelText(readingArea, 'value');
      expect(value).toHaveTextContent('test skill 2');
    });
  });

  describe('keyboard navigation', () => {
    beforeAll(() => {
      const load = () => [
        { id: 1, text: 'test skill 1' },
        { id: 2, text: 'test skill 2' },
        { id: 3, text: 'test skill 3' },
        { id: 4, text: 'test skill 4' },
      ];

      render(
        <Grid>
          <StatefulSkills label="label" onLoad={load} defaultValue={[]} />
          <StatefulSkills label="label" onLoad={load} defaultValue={[]} />
          <StatefulSkills label="label" onLoad={load} defaultValue={[]} />
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

    it('shows skills after enter', () => {
      userEvent.keyboard('{enter}');

      const readingAreas = screen.getAllByLabelText('reading area');

      expect(readingAreas[0]).not.toBeVisible();
      expect(screen.getByText('test skill 1')).toBeInTheDocument();
    });

    it('changes with multiple skills', async () => {
      userEvent.keyboard('{arrowdown}{enter}{enter}');
      userEvent.keyboard('{arrowdown}{enter}{enter}');
      userEvent.keyboard('{arrowdown}{enter}{enter}');
      userEvent.keyboard('{esc}');

      const readingAreas = screen.getAllByLabelText('reading area');
      expect(readingAreas[0]).toBeVisible();

      expect(readingAreas[0]).toBeVisible();
      const value = getByLabelText(readingAreas[0], 'value');
      expect(value).toHaveTextContent('test skill 2');
      expect(value).toHaveTextContent('test skill 3');
      expect(value).toHaveTextContent('test skill 4');
    });
  });
});
