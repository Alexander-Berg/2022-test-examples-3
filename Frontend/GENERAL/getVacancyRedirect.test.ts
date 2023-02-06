import { IRequestData } from '../../typings/apphost';

describe('getVacancyRedirect()', () => {
    describe('testing', () => {
        let getVacancyRedirect: ({}) => string;

        beforeAll(() => {
            getVacancyRedirect = require('./getVacancyRedirect').getVacancyRedirect;
            jest.mock('./uriHelpers', () => ({
                getEnvFlags: () => ({
                    needPreview: true,
                    isTesting: true,
                }),
            }));
        });

        afterEach(() => {
            jest.restoreAllMocks();
            jest.resetModules();
        });

        it('should form redirect', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14' } as IRequestData,
                vacancyData: { title: 'Разработчик Интерфейсов', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/разработчик-интерфейсов-14'));
        });

        it('should remove special symbols', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14' } as IRequestData,
                vacancyData: { title: '<script>alert("hello!")</script>Разработчик-%интерфейсов&', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/script-alert-hello-script-разработчик-интерфейсов-14'));
        });

        it('should preserve hyphen', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14' } as IRequestData,
                vacancyData: { title: 'ML-разработчик—аналитик', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/ml-разработчик-аналитик-14'));
        });

        it('should replace (), [], <>, {}', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14' } as IRequestData,
                vacancyData: { title: '<Разработчик [интерфейсов] (серьёзный) {в Облако} - why not?>', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/разработчик-интерфейсов-серьёзный-в-облако-why-not-14'));
        });

        it('should replace multiple spaces', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14' } as IRequestData,
                vacancyData: { title: '  Разработчик     интерфейсов       ', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/разработчик-интерфейсов-14'));
        });

        it('should redirect incorrectly encoded paths', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/%f1-14' } as IRequestData,
                vacancyData: { title: 'да', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/да-14'));
        });

        it('should not redirect correcly encoded paths', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/%D0%BD%D0%B5%D1%82-15' } as IRequestData,
                vacancyData: { title: 'нет', id: 15 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(null);
        });

        it('should preserve query', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14?foo=bar&bar=baz' } as IRequestData,
                vacancyData: { title: 'Разработчик интерфейсов', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/разработчик-интерфейсов-14?foo=bar&bar=baz'));
        });
    });

    describe('l7test', () => {
        let getVacancyRedirect: ({}) => string;

        beforeAll(() => {
            getVacancyRedirect = require('./getVacancyRedirect').getVacancyRedirect;
            jest.mock('./uriHelpers', () => ({
                getEnvFlags: () => ({
                    needPreview: true,
                    isTesting: false,
                }),
            }));
        });

        afterEach(() => {
            jest.restoreAllMocks();
            jest.resetModules();
        });

        it('should preserve query', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14?foo=bar&bar=baz' } as IRequestData,
                vacancyData: { title: 'Разработчик интерфейсов', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/разработчик-интерфейсов-14?foo=bar&bar=baz'));
        });
    });

    describe('production', () => {
        let getVacancyRedirect: ({}) => string;

        beforeAll(() => {
            getVacancyRedirect = require('./getVacancyRedirect').getVacancyRedirect;
            jest.mock('./uriHelpers', () => ({
                getEnvFlags: () => ({
                    needPreview: false,
                    isTesting: false,
                }),
            }));
        });

        afterEach(() => {
            jest.restoreAllMocks();
            jest.resetModules();
        });

        it('should not preserve query', () => {
            const data = {
                requestData: { uri: '/jobs/vacancies/14?foo=bar&bar=baz' } as IRequestData,
                vacancyData: { title: 'Разработчик интерфейсов', id: 14 } as unknown,
            };

            expect(getVacancyRedirect(data)).toEqual(encodeURI('/jobs/vacancies/разработчик-интерфейсов-14'));
        });
    });
});
