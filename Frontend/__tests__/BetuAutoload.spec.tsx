import * as React from 'react';
import { omit } from 'lodash/fp';
import { shallow } from 'enzyme';
import { Status } from '@yandex-turbo/components/withBWMAutoload/withBWMAutoload';
import { BeruButton } from '@yandex-turbo/components/BeruButton/BeruButton';
import { Loader as BeruAutoload } from '../BeruAutoload';

describe('BeruAutoload', () => {
    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruAutoload />);

        expect(wrapper).toHaveLength(1);
    });

    it('должен рисовать заглушку по дефолту и не вызывать обработчики если не попал во вьюпорт', () => {
        const unobserve = jest.fn();
        const load = jest.fn();
        const wrapper = shallow(<BeruAutoload className="test" unobserve={unobserve} load={load} isVisible={false} status={Status.NOT_STARTED} />);

        expect(unobserve).toHaveBeenCalledTimes(0);
        expect(load).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.beru-autoload')).toHaveLength(1);
        expect(wrapper.hasClass('test')).toBeTruthy();
    });

    it('должен отписываться от обсервера, загружать данные если попал во вьюпорт и рисовать заглушку', () => {
        const unobserve = jest.fn();
        const load = jest.fn();
        const wrapper = shallow(<BeruAutoload unobserve={unobserve} load={load} isVisible={false} status={Status.NOT_STARTED} />);

        // проверяем что раньше времени обработчики не вызываются
        expect(unobserve).not.toHaveBeenCalled();
        expect(load).not.toHaveBeenCalled();

        // симулируем ситуацию, когда блок попадает в видимую область
        wrapper.setProps({ isVisible: true });

        // проверяем что отписались от обсерверя и начали заружать данные
        expect(unobserve).toHaveBeenCalled();
        expect(load).toHaveBeenCalled();
        expect(wrapper.find('.beru-autoload')).toHaveLength(1);
    });

    it('должен вставлять загруженный кусок html после загрузки и прокидывать className', () => {
        const wrapper = shallow(<BeruAutoload className="test" status={Status.LOADED} html='<div class="custom"></div>' />);

        expect(wrapper.render().find('.custom')).toHaveLength(1);
        expect(wrapper.hasClass('test')).toBeTruthy();
    });

    describe('Заглушка', () => {
        it('по умолчанию должна оторажаться дефолтная(stub="default")', () => {
            const wrapper = shallow(<BeruAutoload status={Status.NOT_STARTED} />);

            expect(wrapper.find('.beru-autoload__stub-default')).toHaveLength(1);
        });

        it('должен отображаться однопиксельный разделитель(stub="separator")', () => {
            const wrapper = shallow(<BeruAutoload status={Status.NOT_STARTED} stub="separator" />);

            expect(wrapper.find('.beru-autoload__stub-separator')).toHaveLength(1);
        });

        it('должна отображаться группу заглушек, в зависиомсти от установленного кол-ва', () => {
            const wrapper = shallow(<BeruAutoload status={Status.NOT_STARTED} stub={{ count: 1, type: 'row' }} />);

            expect(wrapper.find('.beru-autoload__stub-row')).toHaveLength(1);

            wrapper.setProps({ stub: { count: 5, type: 'row' } });
            expect(wrapper.find('.beru-autoload__stub-row')).toHaveLength(5);
        });
    });

    it('должна отбражаться кнопка, по нажатию на которую происходит загрузка данных', () => {
        const unobserve = jest.fn();
        const load = jest.fn();
        const buttonProps = {
            text: 'Загрузить езе',
            theme: 'normal',
            fullwidth: true,
        };
        const wrapper = shallow(<BeruAutoload unobserve={unobserve} load={load} status={Status.NOT_STARTED} button={buttonProps} />);
        const button = wrapper.find(BeruButton);
        const props = button.find(BeruButton).props();
        const passedProps = omit('onClick', props);

        // проверяем что пропсы полученные из вне доходят до кнопки
        expect(passedProps).toEqual({ ...buttonProps });
        // проверяем что не явно передается обработчик
        expect(typeof props.onClick).toBe('function');

        // Проверяем что загрузка данных работает и отписка от обсервера
        button.simulate('click');
        expect(unobserve).toHaveBeenCalledTimes(1);
        expect(load).toHaveBeenCalledTimes(1);
    });

    describe('Прокрутка', () => {
        it('должен доскроливать до переданного элемента из свойсва moveTo по DOMContentLoaded', () => {
            const scrollTo = jest.fn();

            shallow(<BeruAutoload status={Status.NOT_STARTED} moveTo=".element" scrollTo={scrollTo} />);
            window.dispatchEvent(new Event('DOMContentLoaded'));

            expect(scrollTo).toHaveBeenCalledWith('.element');
        });

        it('должен доскроливать только один раз, если компонент больше чем 1 на странице', () => {
            const scrollToCalled = jest.fn();
            const scrollToNotCalled = jest.fn();

            // симулируем как будто у нас два компонента на странице и дергаем событие
            shallow(<BeruAutoload status={Status.NOT_STARTED} moveTo=".element" scrollTo={scrollToCalled} />);
            shallow(<BeruAutoload status={Status.NOT_STARTED} moveTo=".element" scrollTo={scrollToNotCalled} />);
            window.dispatchEvent(new Event('DOMContentLoaded'));

            expect(scrollToCalled).toHaveBeenCalledWith('.element');
            expect(scrollToNotCalled).not.toHaveBeenCalled();
        });
    });

    it('должен схлопываться если возникла ошибка', () => {
        // @ts-ignore подменяем статус чтобы показать схлопывание
        const wrapper = shallow(<BeruAutoload status="unknown" />);

        expect(wrapper.isEmptyRender()).toBe(true);
    });
});
