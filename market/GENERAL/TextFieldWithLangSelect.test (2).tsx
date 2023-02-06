import { fireEvent, render, screen } from '@testing-library/react';
import React, { ChangeEvent } from 'react';

import { Language } from 'src/java/definitions';
import { TextFieldWithLangSelect } from './TextFieldWithLangSelect';

jest.mock('src/components/Select/Select.tsx', () => {
  return {
    Select: ({
      options,
      value,
      onChange,
    }: {
      options: Array<{ value: number; label: string }>;
      value: number;
      onChange: (v?: { value: number; label: string }) => void;
    }) => {
      function handleChange(event: ChangeEvent<HTMLSelectElement>) {
        const option = options.find(option => option.value.toString() === event.currentTarget.value);
        onChange(option);
      }

      return (
        <select data-testid="select" value={value} onChange={handleChange}>
          {options.map(({ label, value }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      );
    },
  };
});

describe('<TextFieldWithLangSelect/>', () => {
  it('renders without errors', () => {
    const value = { language: Language.BELARUSSIAN, word: 'testik', morphological: false };

    render(<TextFieldWithLangSelect value={value} onChange={() => 1} />);

    expect(screen.getByDisplayValue('testik')).not.toBeNull();
  });

  it('call change handlers', () => {
    let value = { language: Language.BELARUSSIAN, word: 'testik', morphological: false };

    const view = render(
      <TextFieldWithLangSelect
        value={value}
        onChange={v => {
          value = v;
        }}
      />
    );

    const wordInput = screen.getByDisplayValue('testik');

    const langSelect = screen.getByTestId('select');

    fireEvent.change(wordInput, { target: { value: 'testovich' } });

    view.rerender(
      <TextFieldWithLangSelect
        value={value}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(langSelect, { target: { value: Language.TURKEY } });

    expect(value).toEqual({
      language: 'TURKEY',
      word: 'testovich',
      morphological: false,
    });
  });
});
