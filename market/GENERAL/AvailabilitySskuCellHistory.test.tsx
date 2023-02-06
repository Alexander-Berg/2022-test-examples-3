import React from 'react';
import { render, screen } from '@testing-library/react';

import { DisplayOfferStatusAuditInfo, OfferAvailability } from 'src/java/definitions';
import { AvailabilitySskuCellHistory } from '.';

export const generateHistory = (count: number): DisplayOfferStatusAuditInfo[] => {
  return new Array(count).fill(0).map((_, index) => ({
    newComment: `Коментарий ${index}`,
    newStatus: OfferAvailability.ACTIVE,
    author: `Автор ${index}`,
    modifiedTs: new Date(index.toString()).toISOString(),
  }));
};

describe('<AvailabilitySskuCellHistory/>', () => {
  it('main flow', () => {
    const onShowMore = jest.fn();
    const history = generateHistory(10);
    render(
      <AvailabilitySskuCellHistory
        auditLink=""
        isLoading={false}
        afterTableText="Какой-то текст"
        history={history}
        onShowMore={onShowMore}
      />
    );

    for (let i = 0; i < 10; i++) {
      expect(screen.queryByText(`Коментарий ${i}`)).toBeInTheDocument();
      expect(screen.queryByText(`Автор ${i}`)).toBeInTheDocument();
    }
  });
});
