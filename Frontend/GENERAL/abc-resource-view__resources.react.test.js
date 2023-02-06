import React from 'react';
import { mount } from 'enzyme';

import AbcResourceView__Resources from 'b:abc-resource-view e:resources';

describe('AbcResourceView__Resources', () => {
    it('Should render common view resources', () => {
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

        const wrapper = mount(
            <AbcResourceView__Resources
                key="field-type-granter"
                onSRClick={Function.prototype}
                resources={resources}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
