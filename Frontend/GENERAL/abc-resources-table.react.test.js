import React from 'react';
import { mount } from 'enzyme';
import inherit from 'inherit';

import { SCOPE_PREFIX } from 'tools-access-react-redux-router/src/configs';

import AbcResourcesTable from 'b:abc-resources-table';

import { withRedux } from '~/src/common/hoc';
import { configureStore } from '~/src/abc/react/redux/store';

inherit.self(AbcResourcesTable, {}, {
    getScope() {
        if (this.__base(...arguments)) {
            return this.__base(...arguments);
        }
        return document.body;
    },
});

const store = configureStore({
    initialState: {
        [SCOPE_PREFIX]: '',
    },
    fetcherOptions: {
        fetch: () => Promise.resolve(),
    },
});

const AbcResourcesTableConnected = withRedux(AbcResourcesTable, store);

describe('AbcResourcesTable', () => {
    it('Should render empty resources table', () => {
        const wrapper = mount(
            <AbcResourcesTable
                page={1}
                total_pages={1}
                results={[]}
                onTrClick={jest.fn()}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={jest.fn()}
                ordering={{
                    name: 'ord',
                    desc: false,
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resources table', () => {
        const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(946598400000); // 2000й год

        const wrapper = mount(
            <AbcResourcesTableConnected
                page={1}
                total_pages={2}
                results={[
                    {
                        id: 1,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: ['approve'],
                        resource: {
                            external_id: 'EXT_ID',
                            name: 'RES_NAME',
                            type: {
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            attributes: [],
                        },
                        state: 'state_slug',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [],
                    },
                    {
                        id: 2,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: ['resource_provide'],
                        resource: {
                            external_id: 'EXT_ID',
                            name: 'RES_NAME',
                            type: {
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            attributes: [],
                        },
                        state: 'state_slug',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [],
                    },
                    {
                        id: 3,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: ['edit'],
                        resource: {
                            external_id: 'EXT_ID',
                            name: 'RES_NAME',
                            type: {
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            attributes: [],
                        },
                        state: 'state_slug',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [],
                    },
                    {
                        id: 4,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: ['delete'],
                        resource: {
                            external_id: '',
                            name: 'RES_NAME',
                            type: {
                                is_important: true,
                                category: { name: { ru: 'category' } },
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            link: '/',
                            attributes: [],
                        },
                        state: 'granted',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [],
                    },
                    {
                        id: 5,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: ['delete'],
                        resource: {
                            external_id: '',
                            name: 'RES_NAME',
                            type: {
                                is_important: true,
                                category: { name: { ru: 'category' } },
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            link: '/',
                            attributes: [],
                        },
                        state: 'requested',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [],
                    },
                ]}
                onTrClick={jest.fn()}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={jest.fn()}
                ordering={{
                    name: 'modified_at',
                    desc: false,
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
        dateSpy.mockRestore();
    });

    it('Should render resources table with error', () => {
        const error = new Error();

        error.data = {
            message: {
                ru: 'Текст ru message',
                en: 'Текст en message',
            },
        };

        const wrapper = mount(
            <AbcResourcesTable
                error={error}
                page={1}
                total_pages={1}
                results={[]}
                onTrClick={jest.fn()}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={jest.fn()}
                ordering={{
                    name: 'ord',
                    desc: false,
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should call onTrClick on click table row', () => {
        const onTrClick = jest.fn();
        const id = 42;

        const wrapper = mount(
            <AbcResourcesTableConnected
                page={1}
                total_pages={1}
                results={[
                    {
                        id,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: [],
                        resource: {
                            external_id: 'EXT_ID',
                            name: 'RES_NAME',
                            type: {
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            attributes: [],
                        },
                        state: 'state_slug',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [],
                    },
                ]}
                onTrClick={onTrClick}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={jest.fn()}
                ordering={{
                    name: 'ord',
                    desc: false,
                }}
            />,
        );

        wrapper.find('.abc-resources-table__tr_type_body').simulate('click');
        expect(onTrClick).toHaveBeenCalledWith(id);

        wrapper.unmount();
    });

    it('Should not call onTrClick on link click', () => {
        const onTrClick = jest.fn();
        const id = 42;

        const wrapper = mount(
            <AbcResourcesTableConnected
                page={1}
                total_pages={1}
                results={[
                    {
                        id,
                        modified_at: '2018-01-12T11:25:11.197965Z',
                        service: {
                            slug: 'slug',
                            name: { ru: 'SRV' },
                        },
                        actions: ['edit', 'meta_info', 'delete'],
                        resource: {
                            external_id: 'EXT_ID',
                            name: 'RES_NAME',
                            type: {
                                name: {
                                    ru: 'TYP',
                                },
                                has_editable_tags: false,
                                supplier: {
                                    slug: 'sup',
                                    name: {
                                        ru: 'SUP',
                                    },
                                },
                            },
                            attributes: [],
                        },
                        state: 'state_slug',
                        state_display: { ru: 'State' },
                        tags: [],
                        supplier_tags: [{
                            id: 61,
                            slug: 'production',
                            name: {
                                ru: 'Продакшен',
                            },
                        }],
                    },
                ]}
                onTrClick={onTrClick}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={jest.fn()}
                ordering={{
                    name: 'ord',
                    desc: false,
                }}
            />,
        );

        wrapper
            .find('.abc-resources-table__tr_type_body')
            .find('.abc-resources-table__td_type_supplier')
            .find('.link')
            .simulate('click');

        expect(onTrClick).not.toHaveBeenCalled();

        wrapper.unmount();
    });

    it('Should call onSortClick on desc sort click', () => {
        const onSortClick = jest.fn();

        const wrapper = mount(
            <AbcResourcesTable
                page={1}
                total_pages={2}
                results={[]}
                onTrClick={jest.fn()}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={onSortClick}
                ordering={{
                    name: 'modified_at',
                    desc: true,
                }}
            />,
        );

        wrapper
            .find('.abc-resources-table__tr_type_head')
            .find('.abc-resources-table__th_type_modification-time')
            .find('.link')
            .simulate('click');

        expect(onSortClick).toHaveBeenCalledWith('modified_at');

        wrapper.unmount();
    });

    it('Should call onSortClick on asc sort click', () => {
        const onSortClick = jest.fn();

        const wrapper = mount(
            <AbcResourcesTable
                page={1}
                total_pages={2}
                results={[]}
                onTrClick={jest.fn()}
                onRejectClick={jest.fn()}
                onRemoveClick={jest.fn()}
                onApproveClick={jest.fn()}
                onProvideClick={jest.fn()}
                resourceViewVisible={false}
                resourceEditVisible={false}
                resourceId="42"
                onResourceViewClose={jest.fn()}
                onResourceEditOpen={jest.fn()}
                onResourceEditClose={jest.fn()}
                onResourceViewUpdate={jest.fn()}
                onResourceEditorUpdate={jest.fn()}
                onPagerChange={jest.fn()}
                confirmVisible={false}
                confirmLoading={false}
                onRejectResolve={jest.fn()}
                onSortClick={onSortClick}
                ordering={{
                    name: 'modified_at',
                    desc: false,
                }}
            />,
        );

        wrapper
            .find('.abc-resources-table__tr_type_head')
            .find('.abc-resources-table__th_type_modification-time')
            .find('.link')
            .simulate('click');

        expect(onSortClick).toHaveBeenCalledWith('-modified_at');

        wrapper.unmount();
    });
});
