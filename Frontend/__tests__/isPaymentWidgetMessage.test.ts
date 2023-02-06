import { isPaymentWidgetMessage } from '../PaymentWidget.utils';

test('isPaymentWidgetMessage true', () => {
  expect(isPaymentWidgetMessage('PAYMENT_WIDGET/SUCCESS/close')).toEqual(true);
  expect(isPaymentWidgetMessage('PAYMENT_WIDGET/SUCCESS/watch')).toEqual(true);
});

test('isPaymentWidgetMessage false', () => {
  expect(isPaymentWidgetMessage('{"event":"sandbox:placeholder:pay","eventObject":"{}"}')).toEqual(false);
});
