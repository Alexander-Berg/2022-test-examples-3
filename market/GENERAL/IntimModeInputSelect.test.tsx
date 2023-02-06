import React from 'react';
import { render, screen } from '@testing-library/react';
import selectEvent from 'react-select-event';

import { intimModeOptions } from 'src/pages/CwVerdicts/components/ImageVerdict/ImageVerdict.constants';
import { IntimModeInputSelect } from './IntimModeInputSelect';

const onSelect = jest.fn();

describe('IntimModeInputSelect', () => {
  test.concurrent('select options', async () => {
    render(<IntimModeInputSelect options={intimModeOptions} onSelect={onSelect} />);

    await selectEvent.select(screen.getByRole('textbox'), 'INTIM');

    expect(onSelect).toBeCalledTimes(1);
    expect(onSelect).toBeCalledWith('INTIM');

    await selectEvent.select(screen.getByRole('textbox'), 'FASHION');

    expect(onSelect).toBeCalledTimes(2);
    expect(onSelect).toBeCalledWith('FASHION');
  });
});
