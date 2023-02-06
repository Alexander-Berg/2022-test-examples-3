import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { ProductItemComponent } from '../ProductItem';
import ProductItemAdapter from '../ProductItem.adapter';
import { productItemPropsMock, getAdapterContextMock, getProductItemDataMock } from './datastub';

describe('ProductItem', () => {
    it('Рендерится без ошибок', () => {
        shallow(
            <ProductItemComponent
                {...productItemPropsMock}
            />
        );
    });

    describe('Adapter', () => {
        describe('getButtonsWidthModifier', () => {
            it('Возвращает верный модификатор кнопки', () => {
                const productItemAdapter = new ProductItemAdapter(getAdapterContextMock({}, false));

                productItemAdapter.transform(getProductItemDataMock(undefined));
                const buttonsWidthModifier1 = productItemAdapter.getButtonsWidthModifier();

                productItemAdapter.transform(getProductItemDataMock('list'));
                const buttonsWidthModifier2 = productItemAdapter.getButtonsWidthModifier();

                productItemAdapter.transform(getProductItemDataMock('big-list'));
                const buttonsWidthModifier3 = productItemAdapter.getButtonsWidthModifier();

                expect(buttonsWidthModifier1).toEqual('auto');
                expect(buttonsWidthModifier2).toEqual('auto');
                expect(buttonsWidthModifier3).toEqual('auto');
            });

            it('Возвращает верный модификатор кнопки в эксперименте ecom-design-wide-button', () => {
                const productItemAdapter = new ProductItemAdapter(getAdapterContextMock({
                    'ecom-design-wide-button': 1,
                }, false));

                productItemAdapter.transform(getProductItemDataMock(undefined));

                const buttonsWidthModifier = productItemAdapter.getButtonsWidthModifier();

                expect(buttonsWidthModifier).toEqual('max');
            });
        });
    });
});
