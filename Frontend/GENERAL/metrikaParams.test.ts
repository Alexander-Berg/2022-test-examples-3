import { extendWithMetrikaParams, getVacancyMetrikaParams } from './metrikaParams';
import { ILcRootTurboJSON } from '../../typings/lpc';
import { IPublication } from '../../typings/femida';

const contentMockBase: ILcRootTurboJSON = {
    ograph: {
        title: '',
        description: '',
        image: '',
        site_name: '',
    },
    content: [
        {
            block: 'lc-page',
            content: [],
            analytics: {
                metrika: {
                    ids: [123455],
                    params: {
                        param: '1',
                    },
                },
            },
        },
    ],
    favicon: '',
    yandexVerification: '',
};

export const publicationMockBase: IPublication = {
    id: 1234,
    vacancy: {
        id: 123,
        profession: {
            name: 'profession name',
            slug: 'profession slug',
            professional_sphere: {
                name: 'professional_sphere nam',
                slug: 'professional_sphere slug',
            },
        },
    },
    public_service: {
        id: 37,
        icon: 'icon',
        name: 'service name',
        description: 'service description',
        slug: 'service slug',
        is_active: true,
    },
};

describe('metrikaParams', () => {
    describe('extendWithMetrikaParams', () => {
        it('should extend income turboJson with metrika params', () => {
            expect(extendWithMetrikaParams(contentMockBase, { prof_sphere: 'dev' }).content[0].analytics.metrika).toEqual({
                ids: [123455],
                params: {
                    param: '1',
                    prof_sphere: 'dev',
                },
            });
        });

        it('should`t extend income turboJson when no metrica params provided', () => {
            expect(extendWithMetrikaParams(contentMockBase).content[0].analytics.metrika).toEqual(
                contentMockBase.content[0].analytics.metrika,
            );
        });
    });

    describe('getVacancyMetrikaParams', () => {
        it('should return profsphere, service and profession params', () => {
            expect(getVacancyMetrikaParams(publicationMockBase)).toEqual({
                profession: 'profession slug',
                profsphere: 'professional_sphere slug',
                service: 'service slug',
            });
        });

        it('should return only service param', () => {
            const publicationMock = { ...publicationMockBase };
            if (!publicationMockBase.vacancy) throw new Error();
            const { profession, ...vacancyRest } = publicationMockBase.vacancy;

            publicationMock.vacancy = vacancyRest;

            expect(getVacancyMetrikaParams(publicationMock)).toEqual({
                service: 'service slug',
            });
        });

        it('should return profession and profsphere param', () => {
            const { public_service, ...rest } = publicationMockBase;

            expect(getVacancyMetrikaParams(rest)).toEqual({
                profession: 'profession slug',
                profsphere: 'professional_sphere slug',
            });
        });

        it('should return only profession and service', () => {
            const publicationMock = { ...publicationMockBase };
            if (!publicationMock.vacancy || !publicationMockBase.vacancy?.profession) throw new Error();
            const { professional_sphere, ...professionRest } = publicationMockBase.vacancy.profession;

            publicationMock.vacancy.profession = professionRest;

            expect(getVacancyMetrikaParams(publicationMock)).toEqual({
                profession: 'profession slug',
                service: 'service slug'
            });
        });
    });
});
