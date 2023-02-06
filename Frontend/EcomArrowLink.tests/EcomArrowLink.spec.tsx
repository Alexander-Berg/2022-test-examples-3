import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomArrowLink } from '../EcomArrowLink';

describe('EcomArrowLink компонент', () => {
    it('Должен рендериться без ошибок', () => {
        shallow(<EcomArrowLink text="Перейти в каталог" url="https://yandex.ru" />);
    });
});
