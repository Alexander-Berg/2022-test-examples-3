import * as React from 'react';
import { mount } from 'enzyme';
import { useQueryParams } from '../useQueryParams';

const STUB_KEY = '_location';

jest.mock('react-router-dom', () => ({
    useLocation: () => {
        const key = '_location';
        return global[key];
    },
}));

describe('useScreenQueryParams hook', () => {
    it('Все параметры из списка', () => {
        global[STUB_KEY] = {
            pathname: '/turbo',
            search: '?category_id=123456&category_count=100&product_id=654321&filters=foo=bar&query=песок&about_category_id=13579&sort_by=price',
        };
        const sendParams = jest.fn();

        const Component: React.FC = () => {
            sendParams(useQueryParams());
            return null;
        };

        mount(<Component />);

        expect(sendParams.mock.calls).toEqual([
            [{
                category_id: '123456',
                category_count: '100',
                product_id: '654321',
                filters: 'foo=bar',
                query: 'песок',
                about_category_id: '13579',
                sort_by: 'price',
            }],
        ]);
    });

    it('Все параметры из списка + лишние', () => {
        global[STUB_KEY] = {
            pathname: '/turbo',
            search: '?category_id=123456&category_count=100&product_id=654321&foo=bar&filters=foo=bar&query=песок&about_category_id=13579&sort_by=price',
        };
        const sendParams = jest.fn();

        const Component: React.FC = () => {
            sendParams(useQueryParams());
            return null;
        };

        mount(<Component />);

        expect(sendParams.mock.calls).toEqual([
            [{
                category_id: '123456',
                category_count: '100',
                product_id: '654321',
                filters: 'foo=bar',
                query: 'песок',
                about_category_id: '13579',
                sort_by: 'price',
            }],
        ]);
    });

    it('Часть параметров из списка', () => {
        global[STUB_KEY] = {
            pathname: '/turbo',
            search: '?category_count=100&product_id=654321&about_category_id=13579&sort_by=price',
        };
        const sendParams = jest.fn();

        const Component: React.FC = () => {
            sendParams(useQueryParams());
            return null;
        };

        mount(<Component />);

        expect(sendParams.mock.calls).toEqual([
            [{
                category_count: '100',
                product_id: '654321',
                about_category_id: '13579',
                sort_by: 'price',
            }],
        ]);
    });

    it('Нет параметров', () => {
        global[STUB_KEY] = {
            pathname: '/turbo',
            search: '',
        };
        const sendParams = jest.fn();

        const Component: React.FC = () => {
            sendParams(useQueryParams());
            return null;
        };

        mount(<Component />);

        expect(sendParams.mock.calls).toEqual([[{}]]);
    });
});
