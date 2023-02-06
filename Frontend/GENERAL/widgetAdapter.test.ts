import { widgetAdapter, VAST_ASSET_TYPE_ID, VASTBAST64_ASSET_TYPE_ID } from './index';
import { newMetaFormat } from './mock';

const cloneDeep = require('lodash/cloneDeep');

describe('widgetAdapter', () => {
    it('Простой тест для widget', () => {
        const adaptedData = widgetAdapter(newMetaFormat);
        const preparedMetaToOldFormat = adaptedData.meta;
        const isAdaptedData = adaptedData.adapted;

        expect(isAdaptedData).toEqual(true);
        expect(preparedMetaToOldFormat).toMatchSnapshot();
    });

    // PCODE-20286
    it('Часть блоков уже адаптирована', () => {
        const meta = cloneDeep(newMetaFormat);
        meta.direct.ads.push({
            native: {
                imptrackers: [
                    'https://an.yandex.ru/count'
                ],
                link: 'link_link',
                assets: [
                    {
                        img: {
                            url: 'https://a.d-cd.net/69b7lvyVxbscl_KfcPLOoQ5RVAU-960.jpg',
                            h: 0,
                            w: 0
                        }
                    },
                    {
                        title: {
                            value: 'THE END'
                        }
                    },
                ],
                ext: {
                    beacon: 'https://an.yandex.ru/newscount',
                    rendertracker: 'https://an.yandex.ru/newscount',
                    category: 'Mazda 3',
                    recommendationId: 'Z5FC6F7C6CC9D2CA7',
                    ts: 1641848400
                }
            }
        });

        const adaptedData = widgetAdapter(meta);
        const preparedMetaToOldFormat = adaptedData.meta;
        const isAdaptedData = adaptedData.adapted;

        expect(isAdaptedData).toEqual(true);
        expect(preparedMetaToOldFormat).toMatchSnapshot();
    });

    // https://st.yandex-team.ru/PCODE-20286#61f161dc6f56947f9678d2bc
    // Адаптируем блок как пустой
    it('Блок может быть битым', () => {
        const meta = cloneDeep(newMetaFormat);
        meta.direct.ads.push({
            status: 'error'
        });

        const adaptedData = widgetAdapter(meta);
        const preparedMetaToOldFormat = adaptedData.meta;
        const isAdaptedData = adaptedData.adapted;

        expect(isAdaptedData).toEqual(true);
        expect(preparedMetaToOldFormat).toMatchSnapshot();
    });

    it('Презаданные adfoxPositions и directPositions', () => {
        const meta = cloneDeep(newMetaFormat);
        meta.direct.adfoxPositions = [22];
        meta.direct.directPositions = [23];

        const adaptedData = widgetAdapter(meta);
        const preparedMetaToOldFormat = adaptedData.meta;
        const isAdaptedData = adaptedData.adapted;

        expect(isAdaptedData).toEqual(true);
        expect(preparedMetaToOldFormat.seatbid[0].adfoxPositions).toEqual([22]);
        expect(preparedMetaToOldFormat.seatbid[0].directPositions).toEqual([23]);
    });

    it('Генерация VAST', () => {
        const meta = cloneDeep(newMetaFormat);

        meta.direct.ads[1].bs_data.vast = { campaign_id: '0' };
        meta.direct.ads[1].bs_data.vastBase64 = { campaign_id: '0' };

        const adaptedData = widgetAdapter(meta);
        const assets = adaptedData.meta.seatbid[0].bid[1].adm.native.assets;
        // @ts-ignore
        const vastAsset = assets.find(asset => asset.ext?.type === VAST_ASSET_TYPE_ID);
        // @ts-ignore
        const vastBase64Asset = assets.find(asset => asset.ext?.type === VASTBAST64_ASSET_TYPE_ID);

        expect(vastAsset.data.value.startsWith('<?xml')).toBe(true);
        expect(vastBase64Asset.data.value.startsWith('PD94b')).toBe(true);
    });
});
