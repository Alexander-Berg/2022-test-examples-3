import React from 'react';
import { mount } from 'enzyme';
import inherit from 'inherit';

import AbcResourceView from 'b:abc-resource-view';
import AbcResourceView__AttributeValueContent from 'e:attribute-value-content';

inherit.self(AbcResourceView__AttributeValueContent, {}, {
    getScope() {
        return document.body;
    }
});

inherit.self(AbcResourceView, {}, {
    getScope() {
        return document.body;
    }
});

describe('AbcResourceView', () => {
    it('Should render common resource view', () => {
        const serviceResource = {
            id: 42,
            modified_at: '2018-01-12T11:25:11.197965Z',
            service: {
                slug: 'slug',
                name: { ru: 'SRV' }
            },
            actions: [],
            resource: {
                external_id: 'EXT_ID',
                name: 'RES_NAME',
                type: {
                    name: { ru: 'TYP' },
                    supplier: {
                        slug: 'sup',
                        name: { ru: 'SUP' }
                    }
                },
                attributes: []
            },
            state: 'granted',
            state_display: { ru: 'State' },
            tags: [],
            supplier_tags: [],
            approvers: []
        };

        const wrapper = mount(
            <AbcResourceView
                confirmVisible
                confirmLoading
                onConfirmSubmit={jest.fn()}
                loading={false}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceUsage={{
                    supplierResources: [],
                    consumerResources: []
                }}
                resourceActionMeta={[]}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                serviceResource={serviceResource}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource view with error', () => {
        const resourceViewError = new Error();

        resourceViewError.data = {
            message: {
                ru: 'Текст ru message',
                en: 'Текст en message'
            }
        };

        const wrapper = mount(
            <AbcResourceView
                confirmVisible
                confirmLoading
                onConfirmSubmit={jest.fn()}
                loading={false}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceUsage={{
                    supplierResources: [],
                    consumerResources: []
                }}
                resourceActionMeta={[]}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}

                resourceViewError={resourceViewError}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource view with loading', () => {
        const wrapper = mount(
            <AbcResourceView
                confirmVisible={false}
                confirmLoading={false}
                onConfirmSubmit={jest.fn()}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceActionMeta={[]}
                resourceUsage={{
                    supplierResources: [],
                    consumerResources: []
                }}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}

                loading
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource view with approve and provide actions', () => {
        const serviceResource = {
            id: 42,
            modified_at: '2018-01-12T11:25:11.197965Z',
            service: {
                slug: 'slug',
                name: { ru: 'SRV' }
            },
            actions: ['approve', 'resource_provide'],
            resource: {
                external_id: 'EXT_ID',
                name: 'RES_NAME',
                type: {
                    name: { ru: 'TYP' },
                    supplier: {
                        slug: 'sup',
                        name: { ru: 'SUP' }
                    }
                },
                attributes: []
            },
            state: 'granted',
            state_display: { ru: 'State' },
            tags: [],
            supplier_tags: [],
            approvers: []
        };

        const wrapper = mount(
            <AbcResourceView
                confirmVisible
                confirmLoading
                onConfirmSubmit={jest.fn()}
                loading={false}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceUsage={{
                    supplierResources: [],
                    consumerResources: []
                }}
                resourceActionMeta={[]}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                serviceResource={serviceResource}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource view with reject button', () => {
        const serviceResource = {
            id: 42,
            modified_at: '2018-01-12T11:25:11.197965Z',
            service: {
                slug: 'slug',
                name: { ru: 'SRV' }
            },
            actions: ['delete'],
            resource: {
                external_id: 'EXT_ID',
                name: 'RES_NAME',
                type: {
                    name: { ru: 'TYP' },
                    supplier: {
                        slug: 'sup',
                        name: { ru: 'SUP' }
                    }
                },
                attributes: []
            },
            state: 'requested',
            state_display: { ru: 'State' },
            tags: [],
            supplier_tags: [],
            approvers: []
        };

        const wrapper = mount(
            <AbcResourceView
                confirmVisible
                confirmLoading
                onConfirmSubmit={jest.fn()}
                loading={false}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceUsage={{
                    supplierResources: [],
                    consumerResources: []
                }}
                resourceActionMeta={[]}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                serviceResource={serviceResource}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource view with remove button', () => {
        const serviceResource = {
            id: 42,
            modified_at: '2018-01-12T11:25:11.197965Z',
            service: {
                slug: 'slug',
                name: { ru: 'SRV' }
            },
            actions: ['delete'],
            resource: {
                external_id: 'EXT_ID',
                name: 'RES_NAME',
                type: {
                    name: { ru: 'TYP' },
                    supplier: {
                        slug: 'sup',
                        name: { ru: 'SUP' }
                    }
                },
                attributes: []
            },
            state: 'granted',
            state_display: { ru: 'State' },
            tags: [],
            supplier_tags: [],
            approvers: []
        };

        const wrapper = mount(
            <AbcResourceView
                confirmVisible
                confirmLoading
                onConfirmSubmit={jest.fn()}
                loading={false}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceUsage={{
                    supplierResources: [],
                    consumerResources: []
                }}
                resourceActionMeta={[]}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                serviceResource={serviceResource}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render optional fields', () => {
        const resources = [
            {
                actions: [
                    'edit',
                    'meta_info',
                    'recreate_secret',
                    'restore_secret',
                    'delete_old_secret',
                    'delete'
                ],
                id: 3491477,
                request_id: 352973,
                modified_at: '2018-08-08T13:49:58.158264Z',
                obsolete_id: null,
                resource: {
                    id: 2702044,
                    external_id: '777',
                    type: {
                        category: {
                            id: 5,
                            name: {
                                ru: 'Бузопасность',
                                en: 'Security'
                            },
                            slug: 'security',
                            description: ''
                        },
                        description: {
                            ru: 'Приложение для получения  **TVM тикетов **',
                            en: ''
                        },
                        form_link: 'https://forms.yandex-team.ru/surveys/2637/',
                        has_editable_tags: true,
                        has_multiple_consumers: false,
                        has_supplier_tags: false,
                        has_tags: true,
                        id: 54,
                        is_enabled: true,
                        is_important: false,
                        name: {
                            ru: 'TVM приложение',
                            en: 'TVM приложение'
                        },
                        supplier: {
                            id: 14,
                            slug: 'passp',
                            name: {
                                ru: 'Паспорт',
                                en: 'Passport'
                            },
                            parent: 848
                        },
                        tags: [],
                        usage_tag: null,
                        dependencies: [
                            {
                                id: 88,
                                name: 'OAuth API',
                                supplier: {
                                    id: 14,
                                    slug: 'passp',
                                    name: {
                                        ru: 'Паспорт',
                                        en: 'Passport'
                                    },
                                    parent: 848
                                }
                            },
                            {
                                id: 198,
                                name: 'User Activity API',
                                supplier: {
                                    id: 336,
                                    slug: 'so',
                                    name: {
                                        ru: 'Спамооборона',
                                        en: 'Spamooborona'
                                    },
                                    parent: 871
                                }
                            },
                            {
                                id: 231,
                                name: 'API',
                                supplier: {
                                    id: 303,
                                    slug: 'staff',
                                    name: {
                                        ru: 'Стафф',
                                        en: 'Stafff'
                                    },
                                    parent: 872
                                }
                            },
                            {
                                id: 89,
                                name: 'HistoryDB API',
                                supplier: {
                                    id: 14,
                                    slug: 'passp',
                                    name: {
                                        ru: 'Паспорт',
                                        en: 'Passport'
                                    },
                                    parent: 848
                                }
                            }
                        ]
                    },
                    link: '',
                    name: 'для продакшена',
                    parent: null,
                    attributes: [],
                    obsolete_id: null
                },
                service: {
                    id: 336,
                    slug: 'so',
                    name: {
                        ru: 'Спамооборона',
                        en: 'Spamooborona'
                    },
                    parent: 871
                },
                state: 'granted',
                state_display: {
                    ru: 'Выдан',
                    en: 'Выдан'
                },
                supplier_tags: [],
                tags: [
                    {
                        id: 61,
                        name: {
                            ru: 'Продакшен',
                            en: 'Production'
                        },
                        slug: 'production',
                        type: 'internal',
                        category: {
                            id: 1,
                            name: {
                                ru: 'Окружение',
                                en: 'Environment'
                            },
                            slug: 'environment'
                        },
                        service: null,
                        description: {
                            ru: '',
                            en: ''
                        }
                    },
                    {
                        id: 213,
                        name: {
                            ru: 'Для \'User Activity API\'',
                            en: 'For \'User Activity API\''
                        },
                        slug: 'for-user-activity-api',
                        type: 'internal',
                        category: {
                            id: 3,
                            name: {
                                ru: 'Использование',
                                en: 'Usage'
                            },
                            slug: 'usage'
                        },
                        service: null,
                        description: {
                            ru: '',
                            en: ''
                        }
                    }
                ]
            }
        ];
        const serviceResource = {
            id: 42,
            modified_at: '2018-01-12T11:25:11.197965Z',
            service: {
                slug: 'slug',
                name: { ru: 'SRV' }
            },
            actions: [],
            resource: {
                external_id: 'EXT_ID',
                name: 'RES_NAME',
                type: {
                    name: { ru: 'TYP' },
                    supplier: {
                        slug: 'sup',
                        name: { ru: 'SUP' }
                    }
                },
                attributes: []
            },
            state: 'granted',
            state_display: { ru: 'State' },
            tags: [],
            supplier_tags: [],
            approvers: {
                consumer_approvers: [
                    {
                        login: 'zomb-prj-282',
                        name: { ru: 'Гермес Конрад' },
                        is_dismissed: false
                    }
                ],
                supplier_approvers: [
                    {
                        login: 'zomb-prj-282',
                        name: { ru: 'Гермес Конрад' },
                        is_dismissed: false
                    }
                ]
            },
            consumer_approver: {
                login: 'zomb-prj-282',
                name: { ru: 'Гермес Конрад' },
                is_dismissed: false
            },
            supplier_approver: {
                login: 'zomb-prj-282',
                name: { ru: 'Гермес Конрад' },
                is_dismissed: false
            },
            granter: {
                login: 'zomb-prj-282',
                name: { ru: 'Гермес Конрад' },
                is_dismissed: false
            },
            depriver: {
                login: 'zomb-prj-282',
                name: { ru: 'Гермес Конрад' },
                is_dismissed: false
            }
        };

        const wrapper = mount(
            <AbcResourceView
                confirmVisible
                confirmLoading
                onConfirmSubmit={jest.fn()}
                loading={false}
                popupOpen={false}
                onApprove={jest.fn()}
                onProvide={jest.fn()}
                onReject={jest.fn()}
                onRemove={jest.fn()}
                resourceUsage={{
                    supplierResources: resources,
                    consumerResources: resources
                }}
                resourceActionMeta={[]}
                onAttributeActionClick={jest.fn()}
                onCancelClick={jest.fn()}
                onConsumerTagsEditClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
                onSRClick={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                serviceResource={serviceResource}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
