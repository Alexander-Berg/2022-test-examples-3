import { parsePaymentWidgetMessage } from '../PaymentWidget.utils';
import { EWidgetResult } from '../PaymentWidget.types';

test('parsePaymentWidgetMessage SUCCESS', () => {
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/SUCCESS/close')).toEqual(EWidgetResult.SUCCESS);
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/SUCCESS/watch')).toEqual(EWidgetResult.SUCCESS);
});

test('parsePaymentWidgetMessage ERROR', () => {
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/ERROR/{{error}}/reload')).toEqual(EWidgetResult.ERROR);
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/ERROR/{{error}}/fatal')).toEqual(EWidgetResult.ERROR);
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/ERROR/{{errorCode}}/order')).toEqual(EWidgetResult.ERROR);
});

test('parsePaymentWidgetMessage CLOSE', () => {
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/ERROR/{{error}}/close')).toEqual(EWidgetResult.CLOSE);
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/CLOSE')).toEqual(EWidgetResult.CLOSE);
});

test('parsePaymentWidgetMessage DEFAULT', () => {
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/NEED_AUTH')).toEqual(EWidgetResult.DEFAULT);
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/LOADED')).toEqual(EWidgetResult.DEFAULT);
  expect(parsePaymentWidgetMessage('PAYMENT_WIDGET/UPDATE_HEIGHT/{{height}}')).toEqual(EWidgetResult.DEFAULT);
});
