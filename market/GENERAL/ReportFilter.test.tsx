import React from 'react';
import { render, screen } from '@testing-library/react';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import { ReportFilter } from './ReportFilter';

describe('<ReportFilter />', () => {
  test('correct render', async () => {
    const onChange = jest.fn();
    render(<ReportFilter onChange={onChange} filter={{}} />);

    await selectEvent.select(screen.getByLabelText('Отображается репортом'), ['Да']);
    expect(onChange).toBeCalledWith({ inReport: 'Да' });

    await selectEvent.select(screen.getByLabelText('Содержится в индексе'), ['Да']);
    expect(onChange).toBeCalledWith({ inIndex: 'Да' });

    jest.useFakeTimers();
    userEvent.type(screen.getByPlaceholderText('экспериментальные флаги репорта'), 'vleskin=god;hobbit=Bilbo');
    jest.runAllTimers();
    expect(onChange).toBeCalledWith({ reportRearrFactors: 'vleskin=god;hobbit=Bilbo' });
    jest.useRealTimers();

    expect(onChange).toHaveBeenCalledTimes(3);
  });
});
