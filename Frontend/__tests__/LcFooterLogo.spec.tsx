import * as React from 'react';
import { shallow } from 'enzyme';

import { ILogoProps } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcFooterLogo } from '../LcFooterLogo';

const data: ILogoProps = {
    text: 'Текст лого',
    icon: '//src',
    alt: 'Текст подсказки',
};

describe('Компонент LcFooterLogo', () => {
    it('должен отрендериться', () => {
        const footerLogo = shallow(<LcFooterLogo {...data} />);

        expect(footerLogo.length).toEqual(1);
    });

    it('должен отрендерить картинку определенного размера', () => {
        const footerLogo = shallow(<LcFooterLogo {...data} size={300} />);

        expect(footerLogo.find('.lc-footer-logo__image').html()).toEqual(
            '<img class="lc-footer-logo__image" style="width:300px;height:auto" src="//src" alt="Текст подсказки"/>'
        );
    });
});
