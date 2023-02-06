import React from 'react';
import { render, screen } from '@testing-library/react';

import { PartnerInputViewer } from './PartnerInputViewer';
import { UnitedOffer } from 'src/entities/datacampOffer';
import userEvent from '@testing-library/user-event';

describe('<PartnerInputViewer />', () => {
  it('renders empty data', () => {
    expect(() => {
      render(<PartnerInputViewer data={{}} />);
    }).not.toThrow();
  });

  it('renders without errors', () => {
    const data = {
      basic: { content: { partner: { original_terms: { quantity: { value: 'количество' } } } } },
      service: [
        { key: 333, value: { content: { partner: { original_terms: { quantity: { value: 'количество123' } } } } } },
      ],
    } as unknown as UnitedOffer;

    render(<PartnerInputViewer data={data} />);

    screen.getByText('количество', { exact: false });
    const btn = screen.getByText('Services');
    userEvent.click(btn);
    screen.getByText('#333');
  });
});
