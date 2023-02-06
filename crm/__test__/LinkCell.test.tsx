import { render, screen } from '@testing-library/react';
import React from 'react';

import { LinkCell } from '../Row/Cell/Cells/Simple/LinkCell';
import { LinkCell as ILinkCell } from '../Table.types';

const testText = 'CRM-11119';
const testLink = 'https://st.yandex-team.ru/CRM-11119';

const cell: ILinkCell = {
  id: '1',
  type: 'Link',
  data: {
    link: testLink,
    text: {
      value: testText,
    },
  },
};

describe('LinkCell', () => {
  it('renders link', () => {
    render(<LinkCell cell={cell} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', testLink);
    expect(link).toHaveTextContent(testText);
  });
});
