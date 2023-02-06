import { getPaymentIFrameParams } from '../PaymentWidget.utils';
import { IPlayerMessageEventObject } from '../PaymentWidget.types';

const paymentUrl = '//payment-widget.ott.yandex.ru/?lang=ru&widgetServiceName=ya-sport&widgetSubServiceName=web&serviceId=141';

test('getPaymentIFrameParams plus', () => {
  const eventObject: IPlayerMessageEventObject = {
    monetizationModel: 'SVOD',
    purchaseTag: 'plus',
  };

  const result = '//payment-widget.ott.yandex.ru/?lang=ru&widgetServiceName=ya-sport&widgetSubServiceName=web&serviceId=141&license=SVOD&subscriptionType=plus';

  expect(getPaymentIFrameParams(paymentUrl, eventObject)).toEqual(result);
});

test('getPaymentIFrameParams kp-super-plus', () => {
  const eventObject: IPlayerMessageEventObject = {
    monetizationModel: 'SVOD',
    purchaseTag: 'kp-super-plus',
  };

  const result = '//payment-widget.ott.yandex.ru/?lang=ru&widgetServiceName=ya-sport&widgetSubServiceName=web&serviceId=141&license=SVOD&subscriptionType=kp-super-plus';

  expect(getPaymentIFrameParams(paymentUrl, eventObject)).toEqual(result);
});

test('getPaymentIFrameParams TVOD', () => {
  const eventObject: IPlayerMessageEventObject = {
    monetizationModel: 'TVOD',
    kpId: '1111',
  };

  const result = '//payment-widget.ott.yandex.ru/?lang=ru&widgetServiceName=ya-sport&widgetSubServiceName=web&serviceId=141&license=TVOD&kpId=1111';

  expect(getPaymentIFrameParams(paymentUrl, eventObject)).toEqual(result);
});
