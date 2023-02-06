import moment from 'moment';

import formatDateForInput from '../formatDateForInput';

describe('formatDateForInput', () => {
  it('должен форматировать дату для поля ввода типа date', () => {
    const date = moment('2000-01-01T00:00');
    const inputType = 'date';

    const result = formatDateForInput(date, inputType);

    expect(result).toEqual('2000-01-01');
  });

  it('должен форматировать дату для поля ввода типа datetime-local', () => {
    const date = moment('2000-01-01');
    const inputType = 'datetime-local';

    const result = formatDateForInput(date, inputType);

    expect(result).toEqual('2000-01-01T00:00');
  });

  it('должен принимать дату типа number', () => {
    const date = Number(moment('2000-01-01'));
    const inputType = 'datetime-local';

    const result = formatDateForInput(date, inputType);

    expect(result).toEqual('2000-01-01T00:00');
  });

  it('должен бросать исключение если передан неизвестный тип поля ввода', () => {
    const date = Number(moment('2000-01-01'));
    const inputType = 'boom';

    const target = () => formatDateForInput(date, inputType);

    expect(target).toThrowError(`Unknown input type 'boom'`);
  });
});
