import React from 'react';
import { render, screen } from '@testing-library/react';

import { Title } from './Title';
import { Title as ITitle } from 'src/pages/TaskAudit/types';
import { root } from 'src/test/data/task-audit/task-audit-data.json';

const { title } = root.items[0].items[0];

describe('TaskAudit Title::', () => {
  it('should render the title text', () => {
    render(<Title items={title as ITitle} />);
    expect(screen.getByText('Модель'));
    expect(screen.getByText('499646063'));
    expect(screen.getByText('Londa Professional деми-перманентная крем-краска Ammonia-free, 60 мл'));
  });

  it('should render a child anchor with href', () => {
    render(<Title items={title as ITitle} />);
    expect(
      screen
        .getByText('499646063')
        // eslint-disable-next-line testing-library/no-node-access
        .closest('a')
        ?.getAttribute('href')
    ).toEqual(title[1].url);
  });

  it('should not render anything if items are not an array', () => {
    // eslint-disable-next-line testing-library/render-result-naming-convention
    const app = render(<Title items={(undefined as unknown) as ITitle} />);
    expect(app.container.innerText).toBeFalsy();
  });
});
