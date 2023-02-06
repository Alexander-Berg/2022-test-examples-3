import { BSMetaVMAP } from './typings';
import { normalizeVMAP } from './utils/normalizeVMAP';
import { vmapAdapter } from './vmapAdapter';

describe('vmapAdapter', () => {
    it('should return correct value', () => {
        const bsMeta: BSMetaVMAP = {
            Vmap: {
                AdBreak: [{
                    AdSource: [{
                        AdTagURI: 'AdTagURIValue',
                        AllowMultipleAds: true,
                        Id: '1',
                    }],
                    BreakId: 'preroll',
                    BreakType: 'linear',
                    Extensions: {
                        CountPositions: 2,
                        MaxDuration: 180,
                        MaxRepeatCount: 0,
                    },
                    TimeOffset: 'start',
                }],
                Extensions: {
                    BufferEmptyLimit: 5,
                    BufferFullTimeout: 5000,
                    CategoryID: '0',
                    CategoryName: 'Основной видеоресурс',
                    PageID: '1275620',
                    PartnerID: '30570339',
                    SessionID: '11978542647759405181',
                    SingleVideoSession: false,
                    Skin: 'http://storage.mds.yandex.net/get-partner/14408/default.swf',
                    SkinTimeout: 5000,
                    SkipDelay: 0,
                    SkipTimeLeftShow: false,
                    TimeLeftShow: true,
                    Title: 'Благодаря рекламе это видео для Вас бесплатно',
                    VASTTimeout: 5000,
                    VPAIDEnabled: true,
                    VPAIDTimeout: 5000,
                    VideoTimeout: 5000,
                    WrapperMaxCount: 3,
                    WrapperTimeout: 5000,
                },
            },
        };
        const expectedResult = normalizeVMAP(`
            <?xml version="1.0" encoding="UTF-8"?>
            <vmap:VMAP xmlns:vmap="http://www.iab.net/videosuite/vmap" version="1" ya-uniformat="true">
            <vmap:AdBreak breakType="linear" breakId="preroll" timeOffset="start">
                <vmap:AdSource allowMultipleAds="true" id="1">
                    <vmap:AdTagURI><![CDATA[AdTagURIValue]]></vmap:AdTagURI>
                </vmap:AdSource>
                <vmap:TrackingEvents/>
                <vmap:Extensions>
                    <vmap:Extension type="MaxDuration">180</vmap:Extension>
                    <vmap:Extension type="MaxPositionsCount">2</vmap:Extension>
                    <vmap:Extension type="MaxRepeatCount">0</vmap:Extension>
                </vmap:Extensions>
            </vmap:AdBreak>
            <vmap:Extensions>
                <vmap:Extension type="SkipTimeLeftShow">false</vmap:Extension>
                    <vmap:Extension type="TimeLeftShow">true</vmap:Extension>
                    <vmap:Extension type="VPAIDEnabled">true</vmap:Extension>
                    <vmap:Extension type="BufferEmptyLimit">5</vmap:Extension>
                    <vmap:Extension type="BufferFullTimeout">5000</vmap:Extension>
                    <vmap:Extension type="PartnerID">30570339</vmap:Extension>
                    <vmap:Extension type="Skin">http://storage.mds.yandex.net/get-partner/14408/default.swf</vmap:Extension>
                    <vmap:Extension type="SkinTimeout">5000</vmap:Extension>
                    <vmap:Extension type="SkipDelay">0</vmap:Extension>
                    <vmap:Extension type="Title">Благодаря рекламе это видео для Вас бесплатно</vmap:Extension>
                    <vmap:Extension type="VASTTimeout">5000</vmap:Extension>
                    <vmap:Extension type="VideoTimeout">5000</vmap:Extension>
                    <vmap:Extension type="VPAIDTimeout">5000</vmap:Extension>
                    <vmap:Extension type="WrapperMaxCount">3</vmap:Extension>
                    <vmap:Extension type="WrapperTimeout">5000</vmap:Extension>
                    <vmap:Extension type="CategoryName">Основной видеоресурс</vmap:Extension>
                    <vmap:Extension type="PageID">1275620</vmap:Extension>
                    <vmap:Extension type="CategoryID">0</vmap:Extension>
                    <vmap:Extension type="SessionID">11978542647759405181</vmap:Extension>
                    <vmap:Extension type="SingleVideoSession">false</vmap:Extension>
                </vmap:Extensions>
            </vmap:VMAP>
        `);
        const result = vmapAdapter(bsMeta);

        expect(result).toEqual(expectedResult);
    });
});
