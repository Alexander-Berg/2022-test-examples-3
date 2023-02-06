import {
  ScannerStatus,
  getColorByStatus,
} from '@/components/BarcodeScanner/utils';

describe('Тестирование утилит для компоненты сканера', () => {
  it('Красный цвет для статуса ошибки', () => {
    expect(getColorByStatus(ScannerStatus.error)).toEqual('red');
  });

  it('Зеленый цвет для успешного статуса', () => {
    expect(getColorByStatus(ScannerStatus.success)).toEqual('green');
  });

  it('Белый цвет для других статусов', () => {
    expect(getColorByStatus(ScannerStatus.wait)).toEqual('white');
    expect(getColorByStatus(ScannerStatus.read)).toEqual('white');
  });
});
