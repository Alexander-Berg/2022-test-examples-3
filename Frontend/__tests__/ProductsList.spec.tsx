import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { ProductsList } from '../ProductsList';
import ProductsListAdapter, { SESSION_ID_CGI_NAME } from '../ProductsList.adapter';

const AdapterContextMock = () => ({ data: {} });
const AdapterContextWithSessionIdMock = () => ({
    data: {
        doc: {
            product_listing_info: {
                turbo_session_id: 'bla-bla-id',
            },
        },
    },
});

const AdapterContextWithSessionIdAjaxMock = () => ({
    data: {
        cgidata: {
            args: {
                [SESSION_ID_CGI_NAME]: ['bla-bla-id'],
            },
        },
        doc: {
            product_listing_info: {
                turbo_session_id: 'bla-bla-id-2',
            },
        },
    },
});

describe('ProductsList', () => {
    it('Рендерится без ошибок', () => {
        shallow(
            <div>
                {ProductsList({
                    isAjax: false,
                    page: 0,
                    children: [
                        <div key={1}>1</div>,
                        <div key={2}>2</div>,
                        <div key={3}>3</div>,
                        <div key={4}>4</div>,
                    ],
                })}
            </div>
        );
    });
});

describe('ProductsListAdapter', () => {
    describe('Параметр `turbo_listing_session_id`', () => {
        it('Не добавляется, если нет в данных', () => {
            const productsListAdapter = new ProductsListAdapter(AdapterContextMock());
            const urlData = productsListAdapter.getAutoload(['/turbo?text=breed-shop.ru&page=1']);

            expect(urlData.url)
                .toEqual('/turbo?text=breed-shop.ru&page=1&ajax_type=products-list');
        });

        it('Для первой загрузке в url добавляется параметр, если есть в данных', () => {
            const productsListAdapterWithSessionId = new ProductsListAdapter(AdapterContextWithSessionIdMock());
            const urlData = productsListAdapterWithSessionId.getAutoload(['/turbo?text=breed-shop.ru&page=1']);

            expect(urlData.url)
                .toEqual('/turbo?text=breed-shop.ru&page=1&ajax_type=products-list&turbo_listing_session_id=bla-bla-id');
        });

        it('При ajax-загрузке в url добавляется параметр из текущего урла', () => {
            const productsListAdapterWithSessionIdAjax = new ProductsListAdapter(AdapterContextWithSessionIdAjaxMock());

            productsListAdapterWithSessionIdAjax.isAjax = true;

            const urlData = productsListAdapterWithSessionIdAjax.getAutoload(['/turbo?text=breed-shop.ru&page=2']);

            expect(urlData.url)
                .toEqual('/turbo?text=breed-shop.ru&page=2&ajax_type=products-list&turbo_listing_session_id=bla-bla-id');
        });
    });
});
