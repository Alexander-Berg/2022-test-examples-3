import React from 'react';
import { render, screen, getByLabelText, cleanup } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import { Grid } from '@crm/components/dist/Attribute2/components/Grid';
import { StatefulTags } from './StatefulTags';
import { Tag } from './Tags.types';

jest.mock('modules/tags/containers/connectNewTagModal', () => () => ({ show }) =>
  show ? <div>mock modal</div> : null,
);
jest.mock('modules/tags/components/NewTagModal', () => ({ show }) =>
  show ? <div>mock modal</div> : null,
);

describe('Attribute2/Tags', () => {
  it('renders label', () => {
    render(
      <Grid>
        <StatefulTags label="label" defaultValue={[]} />
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
        <StatefulTags label="label" defaultValue={[{ id: 1, text: 'value', type: 'Personal' }]} />
      </Grid>,
    );

    const readingArea = screen.getByLabelText('reading area');
    const value = getByLabelText(readingArea, 'value');
    expect(value).toBeVisible();
    expect(value).toHaveTextContent('value');

    cleanup();
  });

  describe('after click', () => {
    beforeEach(() => {
      const Tags: Tag[] = [
        { id: 1, text: 'test tag 1', type: 'Personal' },
        { id: 2, text: 'test tag 2', type: 'Personal' },
      ];
      const loadTags = () => Tags;
      render(
        <Grid>
          <StatefulTags label="label" onLoad={loadTags} defaultValue={[]} />
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

    it('shows tags', () => {
      const testSkill2 = screen.getByText('test tag 2');
      expect(testSkill2).toBeVisible();
    });

    it('finishes editing on outside click', () => {
      userEvent.click(document.body);

      const testSkill2 = screen.getByText('test tag 2');
      expect(testSkill2).not.toBeVisible();
    });
  });

  describe('after click on tag', () => {
    beforeEach(() => {
      const Tags: Tag[] = [
        { id: 1, text: 'test tag 1', type: 'Personal' },
        { id: 2, text: 'test tag 2', type: 'Personal' },
      ];
      const loadTags = () => Tags;
      render(
        <Grid>
          <StatefulTags label="label" onLoad={loadTags} defaultValue={[]} />
        </Grid>,
      );

      const readingArea = screen.getByLabelText('reading area');
      const label = getByLabelText(readingArea, 'label');
      userEvent.click(label);

      userEvent.click(screen.getByText('test tag 2'));
    });

    afterEach(() => {
      cleanup();
    });

    it('renders new value', () => {
      const readingArea = screen.getByLabelText('reading area');
      const value = getByLabelText(readingArea, 'value');
      expect(value).toHaveTextContent('test tag 2');
    });
  });

  describe('after click on creation button', () => {
    beforeEach(() => {
      const Tags: Tag[] = [
        { id: 1, text: 'test tag 1', type: 'Personal' },
        { id: 2, text: 'test tag 2', type: 'Personal' },
      ];
      const loadTags = () => Tags;
      render(
        <Grid>
          <StatefulTags label="label" onLoad={loadTags} defaultValue={[]} />
        </Grid>,
      );

      const readingArea = screen.getByLabelText('reading area');
      const label = getByLabelText(readingArea, 'label');
      userEvent.click(label);

      userEvent.click(screen.getByText('Создать метку'));
    });

    afterEach(() => {
      cleanup();
    });

    it('shows modal', () => {
      expect(screen.getByText('mock modal')).toBeInTheDocument();
    });
  });

  describe('keyboard navigation', () => {
    beforeAll(() => {
      const load = (): Tag[] => [
        { id: 1, text: 'test tag 1', type: 'Personal' },
        { id: 2, text: 'test tag 2', type: 'Personal' },
        { id: 3, text: 'test tag 3', type: 'Personal' },
        { id: 4, text: 'test tag 4', type: 'Personal' },
      ];

      render(
        <Grid>
          <StatefulTags label="label" onLoad={load} defaultValue={[]} />
          <StatefulTags label="label" onLoad={load} defaultValue={[]} />
          <StatefulTags label="label" onLoad={load} defaultValue={[]} />
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

    it('shows tags after enter', () => {
      userEvent.keyboard('{enter}');

      const readingAreas = screen.getAllByLabelText('reading area');

      expect(readingAreas[0]).not.toBeVisible();
      expect(screen.getByText('test tag 1')).toBeInTheDocument();
    });

    it('changes with multiple tags', async () => {
      userEvent.keyboard('{arrowdown}{enter}');
      userEvent.keyboard('{enter}');
      userEvent.keyboard('{enter}');
      userEvent.keyboard('{esc}');

      const readingAreas = screen.getAllByLabelText('reading area');
      expect(readingAreas[0]).toBeVisible();

      expect(readingAreas[0]).toBeVisible();
      const value = getByLabelText(readingAreas[0], 'value');
      expect(value).toHaveTextContent('test tag 2');
      expect(value).toHaveTextContent('test tag 3');
      expect(value).toHaveTextContent('test tag 4');
    });
  });
});
