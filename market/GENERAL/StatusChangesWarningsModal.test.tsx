import React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { StatusChangesWarningsModal } from '.';
import { DisplayMskuStatusWarning } from 'src/java/definitions';

const generateWarnings = (count: number): DisplayMskuStatusWarning[] =>
  new Array(count).fill(0).map((_, idx) => ({
    errorMessage: `Ошибка ${idx}`,
    mskuId: idx,
  }));

describe('StatusChangesWarningsModal', () => {
  const warningsCount = 25;
  const warnings = generateWarnings(warningsCount);
  const warningsInfo = {
    warnings,
    totalCount: 100,
  };
  let onSet = jest.fn();
  let onDownload = jest.fn();
  let component: ReturnType<typeof render> = null;

  beforeEach(() => {
    onSet = jest.fn();
    onDownload = jest.fn();
    component = render(
      <StatusChangesWarningsModal
        onDownloadWarnings={onDownload}
        onSetStatusChangesWarnings={onSet}
        warningsInfo={warningsInfo}
      />
    );
  });
  it('main flow', () => {
    for (let i = 0; i < 10; i++) {
      expect(component.getByText(`Ошибка ${i}`)).toBeInTheDocument();
    }
    expect(component.queryByText(`Ошибка 11`)).not.toBeInTheDocument();
    fireEvent.click(component.getByRole('button', { name: 'Да' }));
    expect(onDownload).toHaveBeenCalled();
    fireEvent.click(component.getByRole('button', { name: 'Нет' }));
    expect(onDownload).toHaveBeenCalled();
  });

  it('dont`t download', () => {
    fireEvent.click(component.getByRole('button', { name: 'Нет' }));
    expect(onDownload).not.toHaveBeenCalled();
    expect(onSet).toHaveBeenCalledWith({ warnings: [], totalCount: 0 });
  });
});
