import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { DatePicker } from '@yandex-market/mbo-components';
import ru from 'date-fns/locale/ru';
import { mount, ReactWrapper } from 'enzyme';
import React from 'react';
import { registerLocale } from 'react-datepicker';
import { Textinput } from '@yandex-lego/components/Textinput';

import { EditMode, SeasonsForm, SeasonsFormProps } from './SeasonsForm';

registerLocale('ru', ru);

const defaultOptions: SeasonsFormProps = {
  warehouses: [],
  season: {
    season: { name: '', id: 0, modifiedAt: '' },
    periods: [
      {
        id: 1,
        toMmDd: '12-12',
        fromMmDd: '12-31',
        modifiedAt: '',
        seasonId: 0,
        warehouseId: 0,
      },
    ],
  },
  mode: EditMode.CREATE,
  onSave: console.log,
  onDiscard: console.log,
};

let wrapper: ReactWrapper;

describe('SeasonsForm', () => {
  beforeEach(() => {
    wrapper = mount(<SeasonsForm {...defaultOptions} />);
    wrapper.setProps(defaultOptions);
    wrapper.update();
  });

  afterEach(() => {
    wrapper.unmount();
  });

  // TODO(nikita-stenin): не понимаю, для чего нужно проверять количество инпутов и иконок в форме.
  // Это плохой и не нужный тест по идее
  it('renders correctly', () => {
    expect(wrapper.find(SeasonsForm)).toHaveLength(1);
    // 1 для названия и по 4 на каждый период
    expect(wrapper.find(Textinput)).toHaveLength(9);
    /**
     * - на каждый период 4 иконки календаря и 1 крестик
     * - 1 иконка плюсика
     */
    expect(wrapper.find(FontAwesomeIcon)).toHaveLength(11);
    expect((wrapper.find(DatePicker).at(0).find('input').first().getDOMNode() as HTMLInputElement).value).toEqual(
      '31 декабря'
    );
    expect((wrapper.find(DatePicker).at(1).find('input').first().getDOMNode() as HTMLInputElement).value).toEqual(
      '12 декабря'
    );
  });
});
