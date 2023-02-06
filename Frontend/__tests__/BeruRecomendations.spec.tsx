import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruSnippet } from '@yandex-turbo/components/BeruSnippet/BeruSnippet';
import { BeruRecomendations, IBeruRecomendations } from '../BeruRecomendations';
import * as stubData from './datastub';

describe('компонент BeruRecomendations', () => {
    it('должен отрендериться без падения', () => {
        const data = stubData.defaultRecomendations as IBeruRecomendations;
        const wrapper = shallow(<BeruRecomendations {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    describe('с данными из стаба (defaultRedomendations)', () => {
        const data = stubData.defaultRecomendations as IBeruRecomendations;
        const wrapper = shallow(<BeruRecomendations {...data} />);
        it('должен содержать верный заголовок', () => {
            expect(wrapper.find('.beru-recomendations__title').render().text()).toBe(data.title);
        });
        it('сниппеты вызываются с верными параметрами', () => {
            expect(wrapper.find(BeruSnippet).first().props()).toMatchObject({
                maxTitleLines: data.snippetMaxTitleLines,
                maxWidth: data.snippetMaxWidth,
                suite: data.suite,
            });
        });
    });
});
