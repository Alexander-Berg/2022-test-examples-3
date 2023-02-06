import * as React from 'react';
import { shallow } from 'enzyme';
import { Suggest } from '../Suggest@phone';
import { IProps } from '../Suggest.types';

const props: IProps = {
    // tslint:disable-next-line:max-line-length
    url: 'https://yandex.ru/suggest/suggest-endings?srv=serp_ru_touch&wiz=TrWth&uil=ru&fact=1&v=4&icon=1&mob=1&tpah=1&sn=7&full_text_count=5&bemjson=0&yu=3198051581484770838&history=1',
    title: 'Саджест',
    service: 'turbo_ru_touch',
    value: 'test',
};

describe('Компонент Suggest', () => {
    it('Должен рендериться без ошибок', () => {
        const suggestComponent = shallow(
            <Suggest {...props} />
        );

        expect(suggestComponent.length).toEqual(1);
    });
});
