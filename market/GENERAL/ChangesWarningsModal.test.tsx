import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

import { ChangesWarningsModal } from '.';

type Warning = {
  field: number;
};

const generateWarnings = (count: number): Warning[] => {
  return new Array(count).fill(0).map((_, index) => ({
    field: index,
  }));
};

describe('<ChangesWarningsModal/>', () => {
  it('main flow', () => {
    const onDownloadWarnings = jest.fn();
    const onDiscard = jest.fn();
    const renderFn = jest.fn();
    const warningsCount = 15;
    const totalCount = 25;
    const warnings = generateWarnings(warningsCount);
    render(
      <ChangesWarningsModal
        warnings={warnings}
        totalCount={totalCount}
        onDiscard={onDiscard}
        render={renderFn}
        onDownloadWarnings={onDownloadWarnings}
      />
    );
    expect(renderFn).toHaveBeenCalledWith(warnings.slice(0, 10));
    expect(screen.queryByText('И еще 5 ошибок.')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Да'));
    expect(onDownloadWarnings).toBeCalled();
    fireEvent.click(screen.getByText('Нет'));
    expect(onDiscard).toHaveBeenCalled();
  });

  it('render null on empty warnings', () => {
    const component = render(
      <ChangesWarningsModal
        totalCount={10}
        warnings={[]}
        onDiscard={jest.fn()}
        render={jest.fn()}
        onDownloadWarnings={jest.fn()}
      />
    );
    expect(component.container.firstChild).toBeNull();
  });

  test.each([
    [5, 20, 'Успешно сохранено 15 статусов. При сохранении 5 статусов произошла ошибка.'],
    [1, 73, 'Успешно сохранено 72 статуса. При сохранении 1 статуса произошла ошибка.'],
    [1000, 1001, 'Успешно сохранен 1 статус. При сохранении 1000 статусов произошла ошибка.'],
    [1001, 5880, 'Успешно сохранено 4879 статусов. При сохранении 1001 статуса произошла ошибка.'],
    [200, 3781, 'Успешно сохранен 3581 статус. При сохранении 200 статусов произошла ошибка.'],
    [32, 120, 'Успешно сохранено 88 статусов. При сохранении 32 статусов произошла ошибка.'],
  ])('pluralized title with warnings: %i, totalCount: %i ', (warningsCount, totalCount, expected) => {
    const warnings = generateWarnings(warningsCount);
    render(
      <ChangesWarningsModal
        render={jest.fn()}
        warnings={warnings}
        totalCount={totalCount}
        onDiscard={jest.fn()}
        onDownloadWarnings={jest.fn()}
      />
    );
    expect(screen.queryByText(expected)).toBeInTheDocument();
  });
});
