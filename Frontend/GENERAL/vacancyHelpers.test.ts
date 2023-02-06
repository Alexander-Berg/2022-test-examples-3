import { ICity, ISkill, IPublicationService } from '../../typings/femida';
import { getVacancyMetaItem } from './vacancyHelpers';

const cityMock: ICity = {
    name: 'city-name',
    slug: 'city-slug',
};

const skillMock: ISkill = {
    name: 'skill-name',
    id: 123,
};

const serviceMock: IPublicationService = {
    name: 'service-name',
    slug: 'service-slug',
    id: 123,
    is_active: true,
};

describe('vacancyHelpers', () => {
    describe('getVacancyMetaItem', () => {
        it('should return meta item from string', () => {
            const str = 'string-item';

            expect(getVacancyMetaItem(str)).toEqual({
                title: str,
            });
        });

        it('should return meta item from city object', () => {
            expect(getVacancyMetaItem(cityMock)).toEqual({
                title: cityMock.name,
                slug: cityMock.slug,
            });
        });

        it('should return meta item from service object', () => {
            expect(getVacancyMetaItem(serviceMock)).toEqual({
                title: serviceMock.name,
                slug: serviceMock.slug,
            });
        });

        it('should return meta item from skill object', () => {
            expect(getVacancyMetaItem(skillMock)).toEqual({
                title: skillMock.name,
                slug: skillMock.id,
            });
        });
    });
});
