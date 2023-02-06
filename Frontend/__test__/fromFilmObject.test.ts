import { formatImage } from '../fromFilmObject';

describe('Функция formatImage cтавит протокол https и правильно меняет размер', () => {
    const newSize = 'S184x276_2x';
    const newUrl = `https://avatars.mds.yandex.net/get-entity_search/49811/127733403/${newSize}?webp=false`;

    it('Правильно работает для урлов с протоколом', () => {
        expect(
            formatImage('http://avatars.mds.yandex.net/get-entity_search/49811/127733403/S100x100Top', newSize),
        ).toEqual(newUrl);

        expect(
            formatImage('https://avatars.mds.yandex.net/get-entity_search/49811/127733403/S100x100Top', newSize),
        ).toEqual(newUrl);
    });

    it('Правильно работает для урлов без протокола', () => {
        expect(formatImage('//avatars.mds.yandex.net/get-entity_search/49811/127733403/whatever_2x', newSize)).toEqual(
            newUrl,
        );
    });

    it('Правильно работает для урлов без разрешения', () => {
        expect(formatImage('https://avatars.mds.yandex.net/get-entity_search/49811/127733403/', newSize)).toEqual(
            newUrl,
        );
    });

    it('Правильно работает для урлов с разрешением в виде %%', () => {
        expect(formatImage('https://avatars.mds.yandex.net/get-entity_search/49811/127733403/%%', newSize)).toEqual(
            newUrl,
        );
    });

    it('Правильно работает для урлов с разрешением в виде %% без домена 3 уровня "mds"', () => {
        const commonPart = 'avatars.yandex.net/get-music-content/143117/71becd03.a.5576245-1';

        const testUrl = `https://${commonPart}/%%`;
        const expectedUrl = `https://${commonPart}/${newSize}?webp=false`;

        expect(formatImage(testUrl, newSize)).toEqual(expectedUrl);
    });
});
