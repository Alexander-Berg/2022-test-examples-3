// Важно создать элемент раньше импорта константы из platform/core/pageContainer.ts
// Чтобы иметь возможность добавить в этот элемент попап в случае кейса когда страница в iframe

createMockContainer();

import * as React from 'react';
import { shallow, mount } from 'enzyme';
import * as dom from '@yandex-turbo/core/dom';
import { BeruPopup } from '../BeruPopup';

function createMockContainer() {
    const div = document.createElement('div');

    div.setAttribute('class', 'page__container');
    document.body.appendChild(div);
}

describe('BeruPopup', () => {
    let content: React.ReactChild;

    beforeEach(() => {
        content = <div className="child">Content</div>;
    });

    it('не должен рендерится по умолчанию', () => {
        const wrapper = shallow(<BeruPopup anchor={React.createRef()}>{content}</BeruPopup>);

        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('должен вешать className на рут ноду', () => {
        const wrapper = mount(<BeruPopup isOpen className="test-class" anchor={React.createRef()}>{content}</BeruPopup>);

        expect(wrapper.find('.beru-popup').hasClass('test-class')).toBe(true);
    });

    it('должен выводить переданых детей', () => {
        const wrapper = mount(<BeruPopup isOpen className="test-class" anchor={React.createRef()}>{content}</BeruPopup>);

        expect(wrapper.children()).toHaveLength(1);
    });

    it('должен рендерится в body если показывается не в iframe', () => {
        mount(<BeruPopup isOpen anchor={React.createRef()}>{content}</BeruPopup>);

        const beruPopup = document.body.querySelector('.beru-popup');
        const parentNode = beruPopup!.parentNode!.nodeName;

        expect(beruPopup).not.toBeNull();
        expect(parentNode).toBe('BODY');
    });

    it('должен рендереится в контейнере "page__container" если показывается в iframe', () => {
        const isInIframe = jest.spyOn(dom, 'isInIframe').mockReturnValue(true);

        mount(<BeruPopup isOpen anchor={React.createRef()}>{content}</BeruPopup>);

        expect(isInIframe).toHaveBeenCalledTimes(1);
        expect(document.querySelector('.page__container .beru-popup')).not.toBeNull();
    });
});
