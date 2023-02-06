import { VPAID_THEME_INTERACTIVE_VIEWER, VPAID_URL_INTERACTIVE_VIEWER } from '../const';
import { BSMetaVideoAd } from '../typings';
import { normalizeVast } from '../utils/normalizeVast';
import { VASTAdCreator } from '../VASTAdCreator/VASTAdCreator';
import { VASTCreator } from './VASTCreator';

describe('VASTCreator', () => {
    it('should create correct VAST', () => {
        const ad = {
            settings: {
                '2': {
                    linkTail: 'linkTail',
                    viewNotices: ['viewNotice1', 'viewNotice2'],
                },
            },
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            targetUrl: 'bs_data.targetUrl',
                            domain: 'bs_data.domain',
                            bannerFlags: 'bs_data.bannerFlags',
                            bannerLang: 'bs_data.bannerLang',
                            adId: '123',
                            resource_links: {
                                direct_data: {
                                    targetUrl: 'bs_data.resource_links.direct_data.targetUrl',
                                },
                            },
                            count_links: {
                                tracking: 'bs_data.count_links.tracking',
                                falseClick: 'bs_data.count_links.falseClick',
                            },
                            impId: '2',
                        },
                        direct_data: {},
                        constructor_data: {
                            VpaidPcodeUrl: VPAID_URL_INTERACTIVE_VIEWER,
                            Theme: VPAID_THEME_INTERACTIVE_VIEWER,
                            Duration: 15.0,
                            MediaFiles: [
                                {
                                    Id: '1',
                                    Delivery: 'progressive',
                                    Url: 'Url_1',
                                    MimeType: 'webm',
                                    Width: '720',
                                    Height: '480',
                                    Codec: 'vp9',
                                },
                            ],
                            HasAbuseButton: true,
                            SocialAdvertisement: true,
                            PlaybackParameters: {
                                ShowSkipButton: true,
                                SkipDelay: '5',
                            },
                            UseTrackingEvents: true,
                            IsStock: true,
                            InteractiveVpaid: true,
                            AddPixelImpression: true,
                            CreativeId: 'CreativeId',
                            StrmPrefix: 'StrmPrefix',
                            ShowVpaid: true,
                            ShowVideoClicks: true,
                            SoundbtnLayout: '1',
                            AdlabelLayout: '1',
                            CountdownLayout: '1',
                            UseVpaidImpressions: true,
                            AdSystem: 'AdSystem',
                        },
                    },
                },
            },
        } as BSMetaVideoAd;
        const expectedVast = normalizeVast(`
            <?xml version="1.0" encoding="UTF-8"?>
            <VAST version="3.0" ya-uniformat="true">
                <Ad id="a34sdf">
                    <InLine>
                        <AdSystem>AdSystem</AdSystem>
                        <AdTitle>Interactive Direct In Video</AdTitle>
                        <Impression id="rtb"><![CDATA[linkTail]]></Impression>
                        <Impression id="direct"><![CDATA[viewNotice1]]></Impression>
                        <Impression id="view_notice_pixel"><![CDATA[viewNotice1]]></Impression>
                        <Impression id="view_notice_pixel"><![CDATA[viewNotice2]]></Impression>
                        <Impression id="direct_impression_13"><![CDATA[bs_data.count_links.tracking?action-id=13]]></Impression>
                        <Error><![CDATA[https://an.yandex.ru/jserr/3948?errmsg=auto-video-error]]></Error>
                        <Creatives>
                            <Creative id="CreativeId">
                                <Linear skipoffset="00:00:05">
                                    <Duration>00:00:15</Duration>
                                    <TrackingEvents>
                                        <Tracking event="start"><![CDATA[bs_data.count_links.tracking?action-id=0]]></Tracking>
                                        <Tracking event="firstQuartile"><![CDATA[bs_data.count_links.tracking?action-id=1]]></Tracking>
                                        <Tracking event="midpoint"><![CDATA[bs_data.count_links.tracking?action-id=2]]></Tracking>
                                        <Tracking event="thirdQuartile"><![CDATA[bs_data.count_links.tracking?action-id=3]]></Tracking>
                                        <Tracking event="complete"><![CDATA[bs_data.count_links.tracking?action-id=4]]></Tracking>
                                        <Tracking event="mute"><![CDATA[bs_data.count_links.tracking?action-id=5]]></Tracking>
                                        <Tracking event="unmute"><![CDATA[bs_data.count_links.tracking?action-id=6]]></Tracking>
                                        <Tracking event="pause"><![CDATA[bs_data.count_links.tracking?action-id=7]]></Tracking>
                                        <Tracking event="resume"><![CDATA[bs_data.count_links.tracking?action-id=8]]></Tracking>
                                        <Tracking event="skip"><![CDATA[bs_data.count_links.tracking?action-id=9]]></Tracking>
                                    </TrackingEvents>
                                    <MediaFiles>
                                        <MediaFile width="0" height="0" delivery="progressive" type="application/javascript" apiFramework="VPAID"><![CDATA[https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js]]></MediaFile>
                                        <MediaFile width="720" height="480" delivery="progressive" type="webm" codec="vp9"><![CDATA[Url_1]]></MediaFile>
                                    </MediaFiles>
                                    <VideoClicks>
                                        <ClickThrough>bs_data.resource_links.direct_data.targetUrl</ClickThrough>
                                    </VideoClicks>
                                    <AdParameters><![CDATA[{}]]></AdParameters>
                                </Linear>
                                <CreativeExtensions>
                                    <CreativeExtension type="meta">
                                        <yastrm:prefix xmlns:yastrm="http://strm.yandex.ru/schema/vast"><![CDATA[StrmPrefix]]></yastrm:prefix>
                                    </CreativeExtension>
                                </CreativeExtensions>
                            </Creative>
                        </Creatives>
                        <Extensions>
                            <Extension type="controls">
                                <control id="adlabel" layout="1"/>
                                <control id="countdown" layout="1"/>
                                <control id="soundbtn" layout="1"/>
                            </Extension>
                            <Extension type="CustomTracking">
                                <Tracking event="onCreativeInit"><![CDATA[bs_data.count_links.tracking?action-id=11]]></Tracking>
                            </Extension>
                        </Extensions>
                    </InLine>
                </Ad>
            </VAST>
        `);
        const vastCreator = new VASTCreator();
        const vastAdCreator = new VASTAdCreator(ad, {});

        // Удаляем AdParameters. Для них есть отдельные тесты.
        const vastXMLString = vastAdCreator.getXMLString()
            .replace(/<AdParameters>[\s\S]*<\/AdParameters>/, '<AdParameters><![CDATA[{}]]></AdParameters>');

        vastCreator.addAd(vastXMLString);

        const resultVast = normalizeVast(vastCreator.getXMLString());

        expect(resultVast).toEqual(expectedVast);
    });
});
