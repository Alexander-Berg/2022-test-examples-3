import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

import { AvailabilityCellHistoryWrapper } from '.';

describe('<AvailabilityCellHistoryWrapper/>', () => {
  it('main flow', () => {
    const onShowMore = jest.fn();
    const afterTableText = 'Какой то текст';
    render(
      <AvailabilityCellHistoryWrapper
        auditLink=""
        isLoading={false}
        afterTableText={afterTableText}
        onShowMore={onShowMore}
      >
        Контент
      </AvailabilityCellHistoryWrapper>
    );

    expect(screen.queryByText('История')).toBeInTheDocument();
    expect(screen.queryByText('Контент')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Показать ещё'));
    expect(onShowMore).toHaveBeenCalledTimes(1);
    expect(screen.getByText(afterTableText)).toBeInTheDocument();
  });
});
