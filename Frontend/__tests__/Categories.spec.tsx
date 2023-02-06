import * as React from 'react';
import { mount } from 'enzyme';
import {
    CategoryItemTypeLink,
    IProps as ICategoryItemProps,
} from '@yandex-turbo/components/CategoryItem/_Type/CategoryItem_Type_Link';
import { Categories, IProps as ICategoriesProps } from '../Categories';
type CategoriesType = React.ComponentType<ICategoriesProps>;
type CategoryItemElement = React.ReactElement<ICategoryItemProps>;

function getChildrens(count: number): CategoryItemElement[] {
    const childrens = [];
    for (let i = 0; i < count; i++) {
        childrens.push(<CategoryItemTypeLink url="" count={239} key={i}>Телевизоры</CategoryItemTypeLink>);
    }

    return childrens;
}

describe('Categories', () => {
    describe('calcVisibleCount()', () => {
        it('должен вернуть стандартное количество видимых элементов', () => {
            const wrapper = mount<CategoriesType>(
                <Categories>{getChildrens(5)}</Categories>
            );
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(3);
            expect(wrapper.find('.turbo-button.turbo-categories__more').render().text()).toEqual('Показать ещё (2)');
        });

        it('не должен оставлять один скрытый элемент', () => {
            const wrapper = mount<CategoriesType>(
                <Categories cutLimit={3}>{getChildrens(4)}</Categories>
            );
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(4);
            expect(wrapper.find('.turbo-button.turbo-categories__more').exists()).toEqual(false);
        });

        it('должен вернуть корректный результат при малом числе элементов', () => {
            const wrapper = mount<CategoriesType>(
                <Categories cutLimit={5}>{getChildrens(4)}</Categories>
            );
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(4);
            expect(wrapper.find('.turbo-button.turbo-categories__more').exists()).toEqual(false);
        });

        it('должен вернуть новое количество элементов после клика по «Показать ещё»', () => {
            const wrapper = mount<CategoriesType>(
                <Categories cutLimit={3} step={3}>{getChildrens(10)}</Categories>
            );
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(3);
            wrapper.find('.turbo-button.turbo-categories__more').simulate('click');
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(6);
            expect(wrapper.find('.turbo-button.turbo-categories__more').render().text()).toEqual('Показать ещё (4)');
        });

        it('должен вернуть корректное количество элементов после клика по «Показать ещё» при малом числе элементов', () => {
            const wrapper = mount<CategoriesType>(
                <Categories cutLimit={3} step={3}>{getChildrens(5)}</Categories>
            );
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(3);
            wrapper.find('.turbo-button.turbo-categories__more').simulate('click');
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(5);
            expect(wrapper.find('.turbo-button.turbo-categories__more').exists()).toEqual(false);
        });

        it('не должен оставлять один скрытый элемент после клика по «Показать ещё»', () => {
            const wrapper = mount<CategoriesType>(
                <Categories cutLimit={3} step={3}>{getChildrens(7)}</Categories>
            );
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(3);
            wrapper.find('.turbo-button.turbo-categories__more').simulate('click');
            expect(wrapper.find('CategoryItemTypeLink')).toHaveLength(7);
            expect(wrapper.find('.turbo-button.turbo-categories__more').exists()).toEqual(false);
        });
    });
});
