import { setActiveMenuItem } from './setActiveMenuItem';
import { ILcRootTurboJSON, ILcHeaderTurboJSON } from '../../typings/lpc';
import { IRequestData } from '../../typings/apphost';

type LcHeaderTurboJSON = ILcRootTurboJSON<[ILcHeaderTurboJSON]>

describe('setActiveMenuItem()', () => {
    it('should match corresponding url', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Работа в Яндексе',
                            url: '/jobs/',
                        },
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies',
                        },
                        {
                            title: 'Города',
                            url: '/jobs/locations',
                        },
                        {
                            title: 'Сервисы',
                            url: '/jobs/services',
                        },
                        {
                            title: 'Профессии',
                            url: '/jobs/professions',

                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;

        const requestData = { uri: '/jobs/vacancies/vacancy/34' } as IRequestData;
        const expected = [
            {
                title: 'Работа в Яндексе',
                url: '/jobs/',
            },
            {
                title: 'Вакансии',
                url: '/jobs/vacancies',
                active: true,
            },
            {
                title: 'Города',
                url: '/jobs/locations',
            },
            {
                title: 'Сервисы',
                url: '/jobs/services',
            },
            {
                title: 'Профессии',
                url: '/jobs/professions',
            },
        ];
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });

    it('should match most corresponding url', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies',
                        },
                        {
                            title: 'Как подать заявку',
                            url: '/jobs/vacancies/how_to_apply',
                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;

        const requestData = { uri: '/jobs/vacancies/how_to_apply/dev' } as IRequestData;
        const expected = [
            {
                title: 'Вакансии',
                url: '/jobs/vacancies',
            },
            {
                title: 'Как подать заявку',
                url: '/jobs/vacancies/how_to_apply',
                active: true,
            },
        ];
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });

    it('defines main page correctly', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Работа в Яндексе',
                            url: '/jobs',
                        },
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies',
                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;

        const requestData = { uri: '/jobs/' } as IRequestData;
        const expected = [
            {
                title: 'Работа в Яндексе',
                url: '/jobs',
                active: true,
            },
            {
                title: 'Вакансии',
                url: '/jobs/vacancies',
            },
        ];
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });

    it('handles siteMenuItems absence correctly', () => {
        const headerRawData = {
            content: [{
                content: [{}],
            }],
        } as LcHeaderTurboJSON;

        const requestData = { uri: '/jobs/vacancies/how_to_apply/dev' } as IRequestData;
        expect(setActiveMenuItem(headerRawData, requestData)).toEqual(headerRawData);
    });

    it('handles empty siteMenuItems correctly', () => {
        const headerRawData = {
            content: [{
                content: [{}],
            }],
        } as LcHeaderTurboJSON;

        const requestData = { uri: '/jobs/vacancies/how_to_apply/dev' } as IRequestData;
        expect(setActiveMenuItem(headerRawData, requestData)).toEqual(headerRawData);
    });

    it('handles slashes in items correctly', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Работа в Яндексе',
                            url: '/jobs/',
                        },
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies/',
                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;

        const expected = [
            {
                title: 'Работа в Яндексе',
                url: '/jobs/',
            },
            {
                title: 'Вакансии',
                url: '/jobs/vacancies/',
                active: true,
            },
        ];

        const requestData = { uri: '/jobs/vacancies' } as IRequestData;
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });

    it('handles slashes in uri correctly', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Работа в Яндексе',
                            url: '/jobs',
                        },
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies',
                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;
        const expected = [
            {
                title: 'Работа в Яндексе',
                url: '/jobs',
            },
            {
                title: 'Вакансии',
                url: '/jobs/vacancies',
                active: true,
            },
        ];
        const requestData = { uri: '/jobs/vacancies/34/' } as IRequestData;
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });

    it('works correctly with query params', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Работа в Яндексе',
                            url: '/jobs',
                        },
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies',
                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;

        const expected = [
            {
                title: 'Работа в Яндексе',
                url: '/jobs',
            },
            {
                title: 'Вакансии',
                url: '/jobs/vacancies',
                active: true,
            },
        ];
        const requestData = { uri: '/jobs/vacancies/34?foo=bar&bar=baz' } as IRequestData;
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });

    it('works correctly with anchors', () => {
        const headerRawData = {
            content: [{
                content: [{
                    siteMenuItems: [
                        {
                            title: 'Работа в Яндексе',
                            url: '/jobs',
                        },
                        {
                            title: 'Вакансии',
                            url: '/jobs/vacancies',
                        },
                    ],
                }],
            }],
        } as LcHeaderTurboJSON;

        const expected = [
            {
                title: 'Работа в Яндексе',
                url: '/jobs',
            },
            {
                title: 'Вакансии',
                url: '/jobs/vacancies',
                active: true,
            },
        ];
        const requestData = { uri: '/jobs/vacancies/34?foo=bar&bar=baz#anchor' } as IRequestData;
        const actual = setActiveMenuItem(headerRawData, requestData).content[0].content[0].siteMenuItems;
        expect(actual).toEqual(expected);
    });
});
