import React from 'react';
import { fireEvent, render, RenderResult } from '@testing-library/react';

import { SearchControl } from './SearchControl';

describe('<SearchControl />', () => {
  it('renders', async () => {
    const onChange = jest.fn();
    const onIndexChanged = jest.fn();
    const app = render(
      <SearchControl
        totalMatches={2}
        initialText="init"
        onChange={onChange}
        onIndexChanged={onIndexChanged}
        useCounter
      />
    );

    const nextBtn = await app.findByTitle('Следующее вхождение');
    const prevBtn = await app.findByTitle('Предыдущее вхождение');

    fireEvent.click(nextBtn);
    expect(onIndexChanged).lastCalledWith(0);

    fireEvent.click(nextBtn);
    expect(onIndexChanged).lastCalledWith(1);

    fireEvent.click(nextBtn);
    expect(onIndexChanged).lastCalledWith(0);

    fireEvent.click(prevBtn);
    expect(onIndexChanged).lastCalledWith(1);

    fireEvent.click(prevBtn);
    expect(onIndexChanged).lastCalledWith(0);

    const searchInput = await app.findByDisplayValue('init');
    fireEvent.keyDown(searchInput, { key: 'enter' });
    expect(onIndexChanged).lastCalledWith(1);

    fireEvent.change(searchInput, { target: { value: 'test_string098765' } });
    expect(onChange).lastCalledWith('test_string098765');

    const clearButton = await app.findByTitle('Очистить');
    fireEvent.click(clearButton);
    expect(onChange).lastCalledWith('');
  });

  it('renders invalid data', async () => {
    const onChange = jest.fn();
    let app: RenderResult | undefined;
    expect(() => {
      app = render(
        <SearchControl
          totalMatches={undefined as unknown as number}
          initialText={null as unknown as string}
          onChange={onChange}
          onIndexChanged={jest.fn}
        />
      );
    }).not.toThrow();

    const searchInput = await app?.findByPlaceholderText('Поиск...');
    expect(searchInput).toBeTruthy();
    fireEvent.change(searchInput!, { target: { value: 'enter' } });
    expect(onChange).lastCalledWith('enter');
  });
});
