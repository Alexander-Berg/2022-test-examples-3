import * as React from 'react';
import { shallow } from 'enzyme';
import * as counterParams from '@yandex-turbo/applications/beru.ru/helpers/metrika';
import * as goalsUtils from '@yandex-turbo/core/goals';
import { BeruButton } from '@yandex-turbo/components/BeruButton/BeruButton';
import { BeruSnippetBase, IBeruSnippet } from '../BeruSnippet';
import * as stubData from './datastub';

describe('компонент BeruSnippet', () => {
    let addToCart: IBeruSnippet['addToCartButton'];
    const unobserve = jest.fn();
    const extractMetrikaCounterParams = jest.spyOn(counterParams, 'extractMetrikaCounterParams');
    const reachYaMetrikaGoals = jest.spyOn(goalsUtils, 'reachYaMetrikaGoals');

    // Патчим данные коллбэком отписки от IntersectionObserver
    Object.keys(stubData).forEach(prop => {
        stubData[prop].unobserve = unobserve;
    });

    beforeEach(() => {
        BeruSnippetBase.counterParams = Object.create(null);
        extractMetrikaCounterParams.mockClear();
        reachYaMetrikaGoals.mockClear();
        unobserve.mockReset();
        addToCart = {
            url: 'https://beru.ru',
            yaGoals: {
                '1': ['ADD_TO_CART'],
            },
        };
    });

    it('должен отрендериться без падения', () => {
        const data = stubData.defaultSnippet as IBeruSnippet;
        const wrapper = shallow(<BeruSnippetBase {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    it('кнопка "в корзину" должна отображаться и иметь верную конфигурацию', () => {
        const wrapper = shallow(<BeruSnippetBase {...stubData.defaultSnippet as IBeruSnippet } addToCartButton={addToCart} />);
        const addToCartButton = wrapper.find(BeruButton);

        expect(addToCartButton.exists()).toEqual(true);
        expect(addToCartButton.props()).toMatchObject({
            text: 'В корзину',
            url: 'https://beru.ru',
            yaMetrikaGoals: {
                '1': ['ADD_TO_CART'],
            },
            size: 's',
            className: 'beru-snippet__add-to-cart',
        });
    });

    it('кнопка "в корзину" не отображается если товар распродан', () => {
        const data = {
            ...stubData.defaultSnippet,
            price: 0,
        } as IBeruSnippet;
        const wrapper = shallow(
            <BeruSnippetBase
                {...data}
                addToCartButton={addToCart}
            />
        );

        expect(wrapper.find(BeruButton).exists()).toEqual(false);
    });

    it('сниппет должен быть обернут в ссылку если передана опция url', () => {
        const wrapper = shallow(<BeruSnippetBase {...stubData.defaultSnippet as IBeruSnippet } url="https://beru.ru" />);
        const link = wrapper.find('.beru-snippet > LinkLayoutDefault');

        expect(link.exists()).toEqual(true);
        expect(link.props()).toMatchObject({
            url: 'https://beru.ru',
            className: 'beru-snippet__link-block',
        });
    });

    it('должен отписываться от IntersectionObserver, если не передана опция visibilityGoal', () => {
        shallow(<BeruSnippetBase {...stubData.defaultSnippet as IBeruSnippet } />);

        expect(unobserve).toHaveBeenCalledTimes(1);
    });

    it('парамтетры счетчика не должны извлекаться повторно, если они уже есть в BeruSnippetBase.counterParams', async() => {
        const goal = [{ id: '123', name: 'TEST', params: { two: 2 } }];

        extractMetrikaCounterParams.mockResolvedValue({ one: 1 });
        shallow(<BeruSnippetBase {...stubData.defaultSnippet as IBeruSnippet } visibilityGoal={goal} />);
        const result = await Object.keys(BeruSnippetBase.counterParams).reduce(async(acc, prop) => {
            acc[prop] = await BeruSnippetBase.counterParams[prop];

            return acc;
        }, {});

        expect(extractMetrikaCounterParams).toHaveBeenCalledTimes(1);

        shallow(<BeruSnippetBase {...stubData.defaultSnippet as IBeruSnippet } visibilityGoal={goal} />);

        expect(extractMetrikaCounterParams).toHaveBeenCalledTimes(1);
        expect(result).toEqual({ '123': { one: 1 } });
    });

    it('должно отправляться событие видимости компонента в метрику и сразу же отписываться от IntersectionObserver, если он попал на экран', async() => {
        const goal = [{ id: '123', name: 'TEST', params: { two: 2 } }];

        expect(BeruSnippetBase.counterParams).toEqual({});

        extractMetrikaCounterParams.mockResolvedValue({ one: 1 });
        reachYaMetrikaGoals.mockReturnValue(undefined);
        const wrapper = shallow(<BeruSnippetBase {...stubData.defaultSnippet as IBeruSnippet } visibilityGoal={goal} />);

        expect(reachYaMetrikaGoals).not.toHaveBeenCalled();

        wrapper.setProps({ isVisible: true });
        await Promise.all(Object.keys(BeruSnippetBase.counterParams).map(prop => BeruSnippetBase.counterParams[prop]));

        expect(unobserve).toHaveBeenCalledTimes(1);
        expect(reachYaMetrikaGoals).toHaveBeenCalledWith([{
            ...goal[0],
            params: {
                one: 1,
                ...goal[0].params,
            },
        }]);
    });

    describe('с данными из стаба (themeCinco)', () => {
        const data = stubData.themeCinco as IBeruSnippet;
        const wrapper = shallow(<BeruSnippetBase {...data} />);
        it('должен содержать класс c темой cinco', () => {
            expect(wrapper.render().hasClass('beru-snippet_theme_cinco')).toEqual(true);
        });
    });

    describe('с данными из стаба (themeSiete)', () => {
        const data = stubData.themeSiete as IBeruSnippet;
        const wrapper = shallow(<BeruSnippetBase {...data} />);
        it('должен содержать класс c темой siete', () => {
            expect(wrapper.render().hasClass('beru-snippet_theme_siete')).toEqual(true);
        });
        it('должен отрисовать бейдж', () => {
            expect(wrapper.find('.beru-snippet__discount').length).toEqual(1);
        });
    });
});
