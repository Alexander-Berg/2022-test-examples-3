import { getPublicationsList } from './getPublicationsList';

import { IPublication } from '../../typings/femida';

import { getPublications } from '../stubs/femidaPublications';

const extractSkills = (publications: ReturnType<typeof getPublicationsList>) => (
    publications.map(item => item.skills).reduce(
        (skills, skillSet) => skills = [...skills, ...skillSet],
        [],
    )
);

const extractCities = (publications: ReturnType<typeof getPublicationsList>) => (
    publications.map(item => item.cities).reduce(
        (cities, citySet) => cities = [...cities, ...citySet],
        [],
    )
);

describe('getPublicationsList', () => {
    let publications: IPublication[];

    beforeEach(() => {
        publications = getPublications().slice(0, 3);
    });

    it('should define default values if passed publication without optional fields', () => {
        const emptyPublications: IPublication[] = [{
            id: 123,
        }];

        const result = getPublicationsList(emptyPublications)[0];

        expect(result).toStrictEqual({
            skills: [],
            // Выпилить после https://st.yandex-team.ru/VACANCIES-778
            skillsWithSlug: [],
            cities: [],
            // Выпилить после https://st.yandex-team.ru/VACANCIES-778
            citiesWithSlug: [],
            content: '',
            id: 123,
            title: '',
            favorite: false,
            service: '',
            // Выпилить после https://st.yandex-team.ru/VACANCIES-778
            serviceWithSlug: '',
        });
    });

    it('should extract skills from publications', () => {
        const skills = extractSkills(getPublicationsList(publications));

        const expectedSkills = [
            'Java',
        ];
        expect(skills).toEqual(expect.arrayContaining(expectedSkills));
    });

    it('should extract cities from publications', () => {
        const cities = extractCities(getPublicationsList(publications));

        const expectedCities = [
            'Москва', 'Москва', 'Москва', 'Санкт-Петербург', 'Санкт-Петербург', 'Санкт-Петербург', 'Нижний Новгород',
        ];
        expect(cities).toEqual(expect.arrayContaining(expectedCities));
    });

    it('should extract content from publications', () => {
        const contents = getPublicationsList(publications)
            .map(item => item.content);

        const expectedContents = [
            'Вам предстоит координировать работу складов в регионах: управлять процессами в WMS, быстро разбираться с ошибками в ней, проверять корректность и эффективность процессов на складах. Ждем, что вы готовы к командировкам и больше трех лет работали в должности не ниже замруководителя склада.',
            'B2B-платформа Яндекс.Маркета — сервис, который помогает владельцам магазинов эффективно управлять своим бизнесом: принимать, обрабатывать и доставлять заказы, вести финансовую отчетность. Команда дизайна продуктов B2B ищет ведущих дизайнеров с опытом проектирования и запуска сервисов от 3 лет.',
            'Буткемп в Яндекс.Маркете — 8-недельная программа для опытных Java-разработчиков. Ее участники пробуют себя в двух командах Маркета и выбирают подходящую. Приходите к нам, если занимались коммерческой разработкой на Java, умеете тестировать код и готовы к сложным и интересным задачам.',
        ];
        expect(contents).toEqual(expect.arrayContaining(expectedContents));
    });

    it('should extract id from publications', () => {
        const ids = getPublicationsList(publications)
            .map(item => item.id);

        const expectedIds = [
            5474, 5205, 5123,
        ];
        expect(ids).toEqual(expect.arrayContaining(expectedIds));
    });

    it('should extract title from publications', () => {
        const titles = getPublicationsList(publications)
            .map(item => item.title);

        const expectedTitles = [
            'Координатор в Маркет',
            'Ведущий дизайнер продукта в Маркет',
            'Буткемп для разработчиков бэкенда в Маркете',
        ];
        expect(titles).toEqual(expect.arrayContaining(expectedTitles));
    });

    it('should extract favorite from publications', () => {
        const favorites = getPublicationsList(publications)
            .map(item => item.favorite);

        const expectedFavorites = [
            false, false, false,
        ];
        expect(favorites).toEqual(expect.arrayContaining(expectedFavorites));
    });

    it('should extract service from publications', () => {
        const services = getPublicationsList(publications)
            .map(item => item.service);

        const expectedService = [
            'Маркет', 'Маркет', 'Маркет',
        ];
        expect(services).toEqual(expect.arrayContaining(expectedService));
    });
});
