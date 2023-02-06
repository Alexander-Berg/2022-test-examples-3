import * as React from 'react';
import { mount } from 'enzyme';
import 'jest-enzyme';
import { LcJobsLink as SubjectTagsItem } from '@yandex-turbo/components/LcJobsLink/LcJobsLink';
import { cls } from '../LcJobsEntityList.cn';
import { LcJobsEntityListPresenter as Subject } from '../LcJobsEntityList';
import { LcJobsEntityListGroup as SubjectGroup } from '../Group/LcJobsEntityList-Group';
import { LcJobsEntityListItem as SubjectItem } from '../Item/LcJobsEntityList-Item';
import { LcJobsEntityListTags as SubjectTags } from '../Tags/LcJobsEntityList-Tags';
import { ILcJobsEntityListProps as IProps, LcJobsEntityListGroup, LcJobsEntityListItem, LcJobsEntityListService, LcJobsEntityListType } from '../LcJobsEntityList.types';

const SubjectItemText = '.' + cls('item-title');
const SubjectItemIcon = '.' + cls('item-icon');

const URL_BASE = 'https://yandex.ru';

describe('LcJobsEntityList', () => {
    const defaultProps: IProps = {
        type: LcJobsEntityListType.Locations,
        content: [],
    };

    it('renders somehow without crashing', () => {
        const wrapper = mount(<Subject { ...defaultProps } />);

        expect(wrapper.html()).toMatchSnapshot();
    });

    describe('Locations', () => {
        const type = LcJobsEntityListType.Locations;

        const rawGroups: Record<string, LcJobsEntityListGroup> = {
            global: { title: 'Весь мир', data: { countryCode: 'GLOBAL' } },
            kz: { title: 'Казахстан', data: { countryCode: 'KZ' } },
            ru: { title: 'Россия', data: { countryCode: 'RU' } },
            ua: { title: 'Украина', data: { countryCode: 'UA' } },
        };
        const groups = rawGroups as Record<keyof typeof rawGroups, LcJobsEntityListGroup>;

        const rawCities = {
            spb: { name: 'Ленинград', publicationsCount: 80, group: groups.ru, slug: 'saint-petersburg' },
            nsk: { name: 'Новосибирск', publicationsCount: 33, group: groups.ru, slug: 'nsk' },
            ekb: { name: 'Свердловск', publicationsCount: 54, group: groups.ru, slug: 'ekb' },
            msk: { name: 'Москва', publicationsCount: 123, group: groups.ru, slug: 'moscow' },
            kiy: { name: 'Киев', publicationsCount: 12, group: groups.ua, slug: 'kiy' },
            alm: { name: 'Алматы', publicationsCount: 7, group: groups.kz, slug: 'alm' },
            remote: { name: 'Удалённая работа', publicationsCount: 29, group: groups.ru, slug: 'remote' },
            globremote: { name: 'Удалённая', publicationsCount: 25, group: groups.global, slug: 'globremote' },
        };
        const cities = rawCities as Record<keyof typeof rawCities, LcJobsEntityListItem>;

        const rawServices = {
            ynd: { slug: 'yndex', name: 'Бизнес', priority: 30 },
            hrt: { slug: 'hrtec', name: 'ЭйчАрТех', priority: 20 },
            mrk: { slug: 'mrket', name: 'Маркетинг', priority: 15 },
            cld: { slug: 'cloud', name: 'Облако', priority: 10 },
            bnr: { slug: 'bnsys', name: 'Баннерная система', priority: 1 },
            sdc: { slug: 'sdcau', name: 'Беспилотные автомобили', priority: 10 },
            bro: { slug: 'brows', name: 'Браузер', priority: 10 },
            vzg: { slug: 'vzgld', name: 'Взгляд', priority: 10 },
            vid: { slug: 'video', name: 'Видео', priority: 10 },
            int: { slug: 'intra', name: 'Внутренние сервисы', priority: 10 },
            zen: { slug: 'dzenn', name: 'Дзен', priority: 10 },
            dir: { slug: 'dirct', name: 'Директ', priority: 10 },
            dsk: { slug: 'disck', name: 'Диск', priority: 10 },
            edl: { slug: 'edadl', name: 'Edadeal', priority: 10 },
        };
        const services = rawServices as Record<keyof typeof rawServices, LcJobsEntityListService>;

        it('Должен содержать ссылку на город с кол-вом и слагом', () => {
            const props = { ...defaultProps, type, content: [cities.msk] };
            const wrapper = mount(<Subject {...props} />).find(SubjectItem);

            expect(wrapper.find('a')).toHaveProp('href', '/jobs/locations/moscow');
            expect(wrapper.find('a').text()).toMatch(/^Москва\s*123$/);
        });

        it('Должен отображать сервисы, если передаем массив сервисов', () => {
            const props: IProps = {
                ...defaultProps,
                isExpanded: false,
                type,
                content: [
                    { ...cities.msk, services: [services.hrt, services.mrk] },
                    { ...cities.kiy, services: [services.mrk] },
                ],
            };

            const wrapper = mount(<Subject {...props} />).find(SubjectTags)
                .map(v => v.find(SubjectTagsItem).map(w => w.text()));

            expect(wrapper).toMatchSnapshot();
        });

        it('Должен скрывать сервисы, если больше N', () => {
            const props: IProps = {
                ...defaultProps,
                isExpanded: false,
                type,
                content: [{ ...cities.msk, services: Object.values(services) }],
            };

            const wrapper = mount(<Subject {...props} />).find(SubjectTags);

            expect(wrapper).toIncludeText('и другие');
            expect(wrapper.find(SubjectTagsItem)).toHaveLength(12);
        });

        it('Россия, Глобал, Москва, Спб и Удалёнка должны быть в начале', () => {
            const props: IProps = {
                ...defaultProps,
                isExpanded: false,
                type,
                content: [
                    cities.spb,
                    cities.nsk,
                    cities.ekb,
                    cities.msk,
                    cities.kiy,
                    cities.remote,
                    cities.globremote,
                ],
            };

            const wrapper = mount(<Subject {...props} />)
                .find(SubjectItem)
                .find('a')
                .map(v => [v.prop('href'), ...v.children().map(w => w.text())]);

            expect(wrapper).toMatchSnapshot();
        });

        it('У стран должна рисоваться иконка с флагом', () => {
            const props: IProps = {
                ...defaultProps,
                isExpanded: false,
                type,
                content: [cities.msk, cities.kiy, cities.alm],
            };

            const wrapper = mount(<Subject {...props} />);
            expect(wrapper).toContainMatchingElement('img[title]');

            const elems = wrapper.find('img[title]');
            expect(elems.map(e => e.prop('title'))).toEqual(['Россия', 'Украина', 'Казахстан']);
            expect(elems.first().prop('src')).toMatch(/\.svg/);
        });

        it('Должен иметь кликабельный заголовок группы и якорь из countryCode', () => {
            const props: IProps = {
                ...defaultProps,
                isExpanded: false,
                type,
                content: [cities.msk],
            };

            const wrapper = mount(<Subject {...props} />);
            expect(wrapper).toContainMatchingElement('[id="section-RU"]');
            expect(wrapper).toContainMatchingElement('[href="#section-RU"]');
        });

        it('Должен содержать теги со ссылками на вакансии с фильтрацией по городу и сервису', () => {
            const city = cities.msk;
            const cityService = services.mrk;
            const props: IProps = {
                ...defaultProps,
                isExpanded: false,
                type,
                content: [{
                    ...city,
                    services: [cityService],
                }],
            };

            const wrapper = mount(<Subject {...props} />);

            const tagsItemLink = wrapper.find(SubjectTagsItem).find('a');
            expect(tagsItemLink.text()).toBe(cityService.name);

            const { searchParams: tagLinkQueryParams } = new URL(tagsItemLink.prop('href'), URL_BASE);

            expect(tagLinkQueryParams.getAll('cities')).toHaveLength(1);
            expect(tagLinkQueryParams.get('cities')).toBe(city.slug);

            expect(tagLinkQueryParams.getAll('services')).toHaveLength(1);
            expect(tagLinkQueryParams.get('services')).toBe(cityService.slug);
        });
    });

    describe('Professions', () => {
        const type = LcJobsEntityListType.Professions;

        const grps = {
            a: { title: 'A', data: { slug: 'A A', description: 'GRP A DESCRIPTION' } },
            z: { title: 'Z', data: { slug: 'Z Z', description: 'GRP Z DESCRIPTION' } },
        };
        const profs = {
            edu: { slug: 'edu', position: 1, name: 'Образовательные проекты', publicationsCount: 1, group: grps.a },
            alz: { slug: 'alz', position: 0, name: 'Аналитика', publicationsCount: 2, group: grps.z },
            cis: { slug: 'cis', position: 0, name: 'Информационная безопасность', publicationsCount: 3, group: grps.z },
            ops: { slug: 'ops', position: 0, name: 'Эксплуатация сервисов', publicationsCount: 4, group: grps.z },
            dsg: { slug: 'dsg', position: 0, name: 'Дизайн', publicationsCount: 5, group: grps.z },
            dev: { slug: 'dev', position: -1, name: 'Разработка', publicationsCount: 20, group: grps.z, description: 'PROF DESCRIPTION' },
            dvc: { slug: 'dvc', position: 0, name: 'Проектирование обрудования', publicationsCount: 6, group: grps.z },
            scn: { slug: 'scn', position: 1, name: 'Наука', publicationsCount: 7, group: grps.a },
            ntt: { slug: 'ntt', position: 0, name: 'Сетевые технологии', publicationsCount: 8, group: grps.z },
        };

        it('Должен содержать ссылку на профессию (в т.ч. слаг)', () => {
            const props = { ...defaultProps, type, content: [profs.dev] };
            const wrapper = mount(<Subject {...props} />).find(SubjectItem);

            expect(wrapper.find('a')).toHaveProp('href', '/jobs/professions/dev');
            expect(wrapper.find('a').text()).toMatch(/^Разработка\s*20$/);
        });

        it('Должен отображаться дескрипшн под тайтлом для профессии', () => {
            const props = { ...defaultProps, type, content: [profs.dev] };
            const wrapper = mount(<Subject {...props} />).find(SubjectItem);

            expect(wrapper).toIncludeText(profs.dev.description);
        });

        it('Должен иметь кликабельный заголовок группы и якорь из слага', () => {
            const props = { ...defaultProps, type, content: [profs.dev] };
            const wrapper = mount(<Subject {...props} />);

            expect(wrapper).toContainMatchingElement('[id="section-Z-Z"]');
            expect(wrapper).toContainMatchingElement('[href="#section-Z-Z"]');
        });
    });

    describe('Services', () => {
        const type = LcJobsEntityListType.Services;

        const lctns = {
            msk: { slug: 'msk', name: 'Мск', priority: 15 },
            spb: { slug: 'spb', name: 'СПб', priority: 10 },
            ekb: { slug: 'ekb', name: 'Екб', priority: 7 },
            kaz: { slug: 'kaz', name: 'Каз', priority: 7 },
            nov: { slug: 'nov', name: 'Нов', priority: 7 },
            sch: { slug: 'sch', name: 'Соч', priority: 7 },
            sim: { slug: 'sim', name: 'Сим', priority: 5 },
            kiy: { slug: 'kiy', name: 'Киев', priority: 5 },
            alm: { slug: 'alm', name: 'Алм', priority: 5 },
            min: { slug: 'min', name: 'Мин', priority: 3 },
            nnv: { slug: 'nnv', name: 'ННв', priority: 3 },
            remote: { slug: 'remote', name: 'Удалённая работа', priority: 3 },
            globremote: { slug: 'globremote', name: 'Удалённая', priority: 3 },
        };
        const grps = {
            a: { title: 'A' },
            y: { title: 'Y', data: { description: 'GRP A DESCRIPTION' } },
            A: { title: 'А' },
            B: { title: 'Б' },
        };
        const srvcs = {
            apm: { name: 'AppMetrika', icon: 'link-icon-apm', slug: 'appmetrika', publicationsCount: 2, group: grps.a },
            cld: { name: 'Yandex.Cloud', icon: 'link-icon-cld', slug: 'yandexcloud', publicationsCount: 63, group: grps.y },
            ass: { name: 'Авиабилеты', slug: 'aviasales', publicationsCount: 5, group: grps.A },
            aru: { name: 'Авто.ру', slug: 'autoru', publicationsCount: 13, group: grps.A },
            afs: { name: 'Афиша', slug: 'afisha', publicationsCount: 1, group: grps.A },
            bnr: { name: 'Баннеры', slug: 'banners', publicationsCount: 2, group: grps.B },
        };

        it('Должен содержать ссылку на сервис в тексте и иконке', () => {
            const props = { ...defaultProps, type, content: [srvcs.cld] };
            const wrapper = mount(<Subject {...props} />).find(SubjectItem);

            const textLink = wrapper.find(SubjectItemText).find('a');
            expect(textLink).toHaveProp('href', '/jobs/services/yandexcloud');
            expect(textLink.text()).toMatch(/^Yandex.Cloud\s*63$/);

            const iconLink = wrapper.find(SubjectItemIcon).find('a');
            expect(iconLink).toHaveProp('href', '/jobs/services/yandexcloud');
            expect(iconLink.find('img')).toHaveProp('src', srvcs.cld.icon);
            expect(iconLink.find('img')).toHaveProp('alt', srvcs.cld.name);
        });

        it('Должен содержать только одну группы (сервисы не группируются)', () => {
            const props = { ...defaultProps, type, content: Object.values(srvcs) };
            const groups = mount(<Subject {...props} />).find(SubjectGroup);

            expect(groups.getElements()).toHaveLength(1);
        });

        it('Должен содержать теги со ссылками на страницу с фильтрацией по сервису и городу', () => {
            const service = srvcs.aru;
            const serviceCity = lctns.ekb;
            const props = {
                ...defaultProps,
                type,
                content: [
                    { ...service, cities: [serviceCity] }
                ]
            };
            const wrapper = mount(<Subject {...props} />).find(SubjectItem);

            const tagsItemLink = wrapper.find(SubjectTagsItem).find('a');
            expect(tagsItemLink.text()).toBe(serviceCity.name);

            const { searchParams: tagLinkQueryParams } = new URL(tagsItemLink.prop('href'), URL_BASE);

            expect(tagLinkQueryParams.getAll('services')).toHaveLength(1);
            expect(tagLinkQueryParams.get('services')).toBe(service.slug);

            expect(tagLinkQueryParams.getAll('cities')).toHaveLength(1);
            expect(tagLinkQueryParams.get('cities')).toBe(serviceCity.slug);
        });

        it('Должен скрывать города, если больше N', () => {
            const props: IProps = {
                ...defaultProps,
                type,
                content: [
                    { ...srvcs.cld, cities: Object.values(lctns) }
                ],
            };

            const wrapper = mount(<Subject {...props} />);
            const tags = wrapper.find(SubjectTags);

            expect(tags).toIncludeText('и другие');
            expect(tags.find(SubjectTagsItem)).toHaveLength(12);
        });
    });
});
