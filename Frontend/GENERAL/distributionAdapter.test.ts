import {
    adapter,
} from './index';

import { meta } from './mock';

describe('distributionAdapter', () => {
    const templates = Object.keys(meta);

    templates.forEach(template => {
        it(`Новый формат адаптируем. Шаблон дистрибуции ${template}`, () => {
            const adaptedData = adapter(meta[template].newMeta);

            const preparedMetaToOldFormat = adaptedData.meta;
            const isAdaptedData = adaptedData.adapted;

            expect(isAdaptedData).toEqual(true);
            expect(preparedMetaToOldFormat).toEqual(meta[template].oldMeta);
        });

        it(`Старый формат не адаптируем. Шаблон дистрибуции ${template}`, () => {
            const adaptedData = adapter(meta[template].oldMeta);

            const preparedMetaToOldFormat = adaptedData.meta;
            const isAdaptedData = adaptedData.adapted;

            expect(isAdaptedData).toEqual(false);
            expect(preparedMetaToOldFormat).toEqual(meta[template].oldMeta);
        });
    });
});
