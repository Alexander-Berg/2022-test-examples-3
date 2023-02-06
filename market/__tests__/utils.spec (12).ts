import { OrderPaymentType } from '@/apollo/generated/graphql';
import { formatPaymentType } from '@/components/OrderPaymentTypeText/utils';

describe('Тестирование утилит для компоненты OrderPaymentType', () => {
  it("Если payment type = Cash, то получаем текст = 'Наличными'", () => {
    expect(formatPaymentType(OrderPaymentType.Cash)).toEqual('Наличными');
  });

  it("Если payment type = Card, то получаем текст = 'Картой'", () => {
    expect(formatPaymentType(OrderPaymentType.Card)).toEqual('Картой');
  });

  it("Если payment type = Prepaid, то получаем пустой текст 'Предоплата'", () => {
    expect(formatPaymentType(OrderPaymentType.Prepaid)).toEqual('Предоплата');
  });

  it('Если payment type = Unknown, то получаем пустой текст', () => {
    expect(formatPaymentType(OrderPaymentType.Unknown)).toEqual('');
  });
});
