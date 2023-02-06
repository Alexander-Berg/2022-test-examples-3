import {makeSuite, makeCase} from 'ginny';

const EXTERNAL_VACANCIES_HOSTNAME = 'yandex.ru';
const EXTERNAL_VACANCIES_PATHNAME = '/jobs/services/market/about';

export default makeSuite('Ссылка "Маркет нанимает"', {
    feature: 'Ссылка "Маркет нанимает"',
    story: {
        'По умолчанию': {
            'Ведет на страницу "Вакансии Яндекс.Маркет"': makeCase({
                issue: 'MARKETFRONT-42335',
                async test() {
                    const actualPath = await this.parent.getVacanciesUrl();

                    return this.expect(actualPath).to.be.link({
                        hostname: EXTERNAL_VACANCIES_HOSTNAME,
                        pathname: EXTERNAL_VACANCIES_PATHNAME,
                    }, {
                        mode: 'equal',
                        skipProtocol: true,
                        skipQuery: true,
                    });
                },
            }),
        },
    },
});
