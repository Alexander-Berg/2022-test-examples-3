import { getVacancyLink } from '../LcJobsVacancyCard.utils';

describe('getVacancyLink()', () => {
    it('should form url from id and title', () => {
        const data = {
            id: '14',
            title: 'Разработчик Интерфейсов',
        };

        expect(getVacancyLink(data)).toEqual('/jobs/vacancies/разработчик-интерфейсов-14');
    });

    it('should remove special symbols', () => {
        const data = {
            id: '14',
            title: '<script>alert("hello!")</script>Разработчик-%интерфейсов&',
        };

        expect(getVacancyLink(data)).toEqual('/jobs/vacancies/script-alert-hello-script-разработчик-интерфейсов-14');
    });

    it('should preserve hyphen', () => {
        const data = {
            id: '14',
            title: 'ML-разработчик—аналитик',
        };

        expect(getVacancyLink(data)).toEqual('/jobs/vacancies/ml-разработчик-аналитик-14');
    });

    it('should replace (), [], <>, {}', () => {
        const data = {
            id: '14',
            title: '<Разработчик [интерфейсов] (серьёзный) {в Облако} - why not?>',
        };

        expect(getVacancyLink(data)).toEqual('/jobs/vacancies/разработчик-интерфейсов-серьёзный-в-облако-why-not-14');
    });

    it('should replace multiple spaces', () => {
        const data = {
            id: '14',
            title: '  Разработчик                интерфейсов   >',
        };

        expect(getVacancyLink(data)).toEqual('/jobs/vacancies/разработчик-интерфейсов-14');
    });
});
