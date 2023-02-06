import { IAdapterContext } from '@yandex-turbo/types/AdapterContext';
import { deepExtend } from '@yandex-turbo/core/utils/deepExtend';
import { IProps } from '../Embed';
import { EmbedTypeDefaultHtml, EmbedTypeDefaultSrc } from '../_type/Embed_type_default.adapter';
import { EmbedTypeFacebook } from '../_type/Embed_type_facebook.adapter';
import { EmbedTypeClipiki } from '../_type/Embed_type_clipiki.adapter';
import { normalizeIframeParams, isVersionAtLeast } from '../SupportedParams/Embed-SupportedParams.adapter';
import {
    defineRatio,
    getSandboxContentByType,
    isSimpleType,
    paramsForDefaultSrcIframe,
    prepareEmbedProps,
    getEmbedAdapterType,
} from '../PrepareProps/Embed-PrepareProps.adapter';
import EmbedAdapter from '../Embed.adapter';

jest.mock('../_type/Embed_type_default.adapter', () => ({
    EmbedTypeDefaultHtml: jest.fn(() => 'content'),
    EmbedTypeDefaultSrc: jest.fn(() => 'content'),
}));

jest.mock('../_type/Embed_type_facebook.adapter', () => ({
    EmbedTypeFacebook: jest.fn(() => 'content'),
}));

jest.mock('../_type/Embed_type_clipiki.adapter', () => ({
    EmbedTypeClipiki: jest.fn(() => 'content'),
}));

jest.mock('../_type/Embed_type_vh.adapter', () => ({
    EmbedTypeVh: jest.fn(() => 'content'),
    isVhPlayer: jest.fn(() => true),
}));

const AdapterContextMock = jest.fn<IAdapterContext>((embedExpFlag = 0) => ({
    assets: {
        generateId: jest.fn(() => 123),
        getBundleJS: jest.fn(),
        getBundleCSS: jest.fn(),
        getBemBlockJs: jest.fn(),
        getPlatform: jest.fn(),
        getCommonStaticPath: () => '//yastatic.net',
        getStore: () => ({
            getState() {
                return {};
            },
        }),
        pushStore: jest.fn(),
    },
    data: {
        reqdata: {
            device_detect: {
                OSFamily: 'ios',
                BrowserName: 'safari',
            },
        },
        cgidata: {
            args: {},
        },
    },
    expFlags: {
        'embed-sandbox-optimization': embedExpFlag,
    },
}));

const embedAdapter = new EmbedAdapter(AdapterContextMock());

describe('Embed adapter', () => {
    describe('transform', () => {
        it('should return null for unsupported type', () => {
            // @ts-ignore - невалидные по ts данные
            const transform = embedAdapter.transform({ content: { bla: { params: 'ddd' } } });

            expect(transform).toBeNull();
        });

        it('should return null for unsupported subtype', () => {
            // @ts-ignore - невалидные по ts данные
            const transform = embedAdapter.transform({ content: { default: { bla: { params: 'ddd' } } } });

            expect(transform).toBeNull();
        });
    });

    describe('defineRatio', () => {
        it('should return default value', () => {
            const ratio = defineRatio('default', 'html');

            expect(ratio).toEqual(0.5625);
        });

        it('should return custom ratio for special type/subtype', () => {
            const ratio = defineRatio('instagram', 'post');

            expect(ratio).toEqual(1.135135135135135);
        });

        it('should return calculated ratio', () => {
            const ratio = defineRatio('instagram', 'post', 10, 5);

            expect(ratio).toEqual(0.5);
        });
    });

    describe('isSimpleType', () => {
        it('should return true if all type is simple', () => {
            const isSimple = isSimpleType('clipiki', 'video');

            expect(isSimple).toEqual(true);
        });

        it('should return true if subtype of type is simple', () => {
            const isSimple = isSimpleType('ok', 'video');

            expect(isSimple).toEqual(true);
        });

        it('should return false if subtype of type is not simple', () => {
            const isSimple = isSimpleType('ok', 'group');

            expect(isSimple).toEqual(false);
        });

        it('should return false if type is not simple', () => {
            const isSimple = isSimpleType('facebook', 'button');

            expect(isSimple).toEqual(false);
        });
    });

    describe('getSandboxContentByType', () => {
        it('should call appropriate params processor for default/html', () => {
            const getSandboxContentByTypeMockIProps = jest.fn<IProps>(() => ({
                type: 'default',
                subtype: 'html',
            }));

            getSandboxContentByType(getSandboxContentByTypeMockIProps(), 'bla', AdapterContextMock());

            expect(EmbedTypeDefaultHtml).toHaveBeenCalled();
        });

        it('should call appropriate params processor for complex type', () => {
            const getSandboxContentByTypeMockIProps = jest.fn<IProps>(() => ({
                type: 'facebook',
                subtype: 'group',
            }));

            getSandboxContentByType(getSandboxContentByTypeMockIProps(), {}, AdapterContextMock());

            expect(EmbedTypeFacebook).toHaveBeenCalled();
        });

        it('should call default params processor for simple type', () => {
            const getSandboxContentByTypeMockIProps = jest.fn<IProps>(() => ({
                type: 'clipiki',
                subtype: 'video',
            }));

            getSandboxContentByType(getSandboxContentByTypeMockIProps(), {}, AdapterContextMock());

            expect(EmbedTypeDefaultSrc).toHaveBeenCalled();
        });
    });

    describe('paramsForDefaultSrcIframe', () => {
        it('should pass src as param for default type', () => {
            const params = paramsForDefaultSrcIframe('default', 'src', 'https://ya.ru');

            expect(params).toEqual({ src: 'https://ya.ru' });
        });

        it('should process iframe params to lowerCase', () => {
            const params = paramsForDefaultSrcIframe(
                'default',
                'src',
                'https://ya.ru',
                {
                    allowFullScreen: 'yes',
                    scrolling: 'no',
                });

            expect(params).toEqual({ src: 'https://ya.ru', allowfullscreen: 'yes', scrolling: 'no' });
        });

        it('should call appropriate params processor for simple types', () => {
            paramsForDefaultSrcIframe('clipiki', 'video', {});

            expect(EmbedTypeClipiki).toHaveBeenCalled();
        });
    });

    describe('normalizeIframeParams', () => {
        it('should filter supported iframe properties and convert to react naming', () => {
            const params = normalizeIframeParams({
                allowfullscreen: 'yes',
                blabla: 'bla',
                scrolling: 'no',
            });

            expect(params).toEqual({ allowFullScreen: 'yes', scrolling: 'no' });
        });
    });

    describe('prepareEmbedProps', () => {
        it('should return correct params with sandbox optimization turned off', () => {
            const params = prepareEmbedProps(
                'default',
                'src',
                { block: 'embed', content: { default: { src: 'yandex.ru' } } },
                AdapterContextMock()
            );

            expect(params).toEqual({
                id: 123,
                isDebug: false,
                isEmbedPerformanceExp: false,
                loaderText: 'Загрузка',
                ratio: 0.5625,
                subtype: 'src',
                nonlazy: false,
                type: 'default',
                embedType: 'defaultSrc',
                // tslint:disable-next-line:max-line-length
                sandbox: '<iframe allowfullscreen="" scrolling="no" frameBorder="0" allow="autoplay; fullscreen" sandbox="allow-forms allow-scripts allow-top-navigation allow-same-origin allow-presentation allow-popups allow-popups-to-escape-sandbox" src="//yastatic.net/video-player/0xdb28055/pages-common/default/default.html#html=" class="turbo-embed__iframe" style="width:auto;height:auto" data-reactroot=""></iframe>',
            });
        });

        it('should return correct params with sandbox optimization turned on', () => {
            const params = prepareEmbedProps(
                'default',
                'src',
                { block: 'embed', content: { default: { src: 'yandex.ru' } } },
                AdapterContextMock(1)
            );

            expect(params).toEqual({
                id: 123,
                isDebug: false,
                isEmbedPerformanceExp: true,
                loaderText: 'Загрузка',
                ratio: 0.5625,
                subtype: 'src',
                nonlazy: false,
                type: 'default',
                embedType: 'defaultSrc',
                dataAttributes: undefined,
            });
        });

        it('should return correct params for vh player with sandbox optimization turned off', () => {
            const params = prepareEmbedProps(
                'vh',
                'video',
                {
                    block: 'embed',
                    content: {
                        vh: {
                            video: {
                                src: 'yandex.ru',
                            },
                        },
                    },
                },
                AdapterContextMock()
            );

            expect(params).toEqual({
                id: 123,
                loaderText: 'Загрузка',
                ratio: 0.5625,
                isDebug: false,
                isEmbedPerformanceExp: false,
                // tslint:disable-next-line:max-line-length
                sandbox: '<iframe src="yandex.ru" class="turbo-embed__iframe" style="width:auto;height:auto" allowfullscreen="" allow="autoplay; fullscreen" scrolling="no" frameBorder="0" data-reactroot=""></iframe>',
                subtype: 'video',
                nonlazy: true,
                type: 'vh',
                embedType: 'vh',
            });
        });

        it('should return correct params for vh player with sandbox optimization turned on', () => {
            const params = prepareEmbedProps(
                'vh',
                'video',
                {
                    block: 'embed',
                    content: {
                        vh: {
                            video: {
                                src: 'yandex.ru',
                            },
                        },
                    },
                },
                AdapterContextMock(1)
            );

            expect(params).toEqual({
                id: 123,
                loaderText: 'Загрузка',
                ratio: 0.5625,
                isDebug: false,
                isEmbedPerformanceExp: true,
                dataAttributes: {
                    src: 'yandex.ru',
                },
                subtype: 'video',
                nonlazy: true,
                type: 'vh',
                embedType: 'vh',
            });
        });

        it('should return type as loaderText for custom types', () => {
            const params = prepareEmbedProps(
                'clipiki',
                'video',
                { block: 'embed', content: { clipiki: { video: { id: '123' } } } },
                AdapterContextMock()
            );

            expect(params && params.loaderText).toEqual('clipiki');
        });

        it('не должен добавлять embed в store если он есть в cgi параметре embeds', () => {
            const adapterContextMock = deepExtend(AdapterContextMock(1), {
                data: {
                    cgidata: {
                        args: { embeds: ['vh,vk'] },
                    },
                },
            });
            prepareEmbedProps(
                'vh',
                'video',
                { block: 'embed', content: { vh: { video: { src: 'yandex.ru' } } } },
                adapterContextMock,
            );
            prepareEmbedProps(
                'vk',
                'poll',
                { block: 'embed', content: { vk: { poll: { api_id: 'test_id', poll_id: 'test_id' } } } },
                adapterContextMock,
            );

            expect(adapterContextMock.assets.pushStore).not.toHaveBeenCalled();
        });

        it('не должен добавлять embed в store если он уже там есть', () => {
            const adapterContextMock = deepExtend(AdapterContextMock(1), {
                assets: {
                    getStore: () => ({
                        getState: () => ({ embed: { vh: { sandbox: 'test' }, vk: { sandbox: 'test' } } }),
                    }),
                },
            });
            prepareEmbedProps(
                'vh',
                'video',
                { block: 'embed', content: { vh: { video: { src: 'yandex.ru' } } } },
                adapterContextMock,
            );
            prepareEmbedProps(
                'vk',
                'poll',
                { block: 'embed', content: { vk: { poll: { api_id: 'test_id', poll_id: 'test_id' } } } },
                adapterContextMock,
            );

            expect(adapterContextMock.assets.pushStore).not.toHaveBeenCalled();
        });

        it('должен добавить embed в store если его нету ни там, ни в cgi параметре embeds', () => {
            const adapterContextMock = AdapterContextMock(1);

            prepareEmbedProps(
                'vk',
                'poll',
                { block: 'embed', content: { vk: { poll: { api_id: 'test_id', poll_id: 'test_id' } } } },
                adapterContextMock,
            );
            expect(adapterContextMock.assets.pushStore).toHaveBeenCalledWith({
                embed: {
                    // tslint:disable-next-line:max-line-length
                    vk: { sandbox: '<iframe allowfullscreen="" scrolling="no" frameBorder="0" allow="autoplay; fullscreen" sandbox="allow-forms allow-scripts allow-top-navigation allow-same-origin allow-presentation allow-popups allow-popups-to-escape-sandbox" src="//yastatic.net/video-player/0xdb28055/pages-common/default/default.html#html=&lt;div id=&quot;YA_TURBO_EMBED_ID&quot; data-reactroot=&quot;&quot;&gt;&lt;/div&gt;&lt;script data-embed-id=&quot;YA_TURBO_EMBED_ID&quot; data-type=&quot;YA_TURBO_TYPE&quot; data-params=&quot;YA_TURBO_PARAMS&quot; data-reactroot=&quot;&quot;&gt;&lt;/script&gt;&lt;script async=&quot;&quot; defer=&quot;&quot; src=&quot;YA_TURBO_SRC&quot; data-reactroot=&quot;&quot;&gt;&lt;/script&gt;" class="turbo-embed__iframe" style="width:auto;height:auto" data-reactroot=""></iframe>' },
                },
            });

            adapterContextMock.assets.pushStore.mockClear();

            prepareEmbedProps(
                'vh',
                'video',
                { block: 'embed', content: { vh: { video: { src: 'yandex.ru' } } } },
                adapterContextMock,
            );
            expect(adapterContextMock.assets.pushStore).toHaveBeenCalledWith({
                embed: {
                    // tslint:disable-next-line:max-line-length
                    vh: { sandbox: '<iframe src="YA_TURBO_SRC" class="turbo-embed__iframe" style="width:auto;height:auto" allowfullscreen="" allow="autoplay; fullscreen" scrolling="no" frameBorder="0" data-reactroot=""></iframe>' },

                },
            });
        });
    });

    describe('getEmbedAdapterType', () => {
        it('должен вернуть корректный embedType для vh video', () => {
            const embedAdapterType = getEmbedAdapterType('vh', 'video');
            expect(embedAdapterType).toEqual('vh');
        });

        it('должен вернуть корректный embedType для default src', () => {
            const embedAdapterType = getEmbedAdapterType('default', 'src');
            expect(embedAdapterType).toEqual('defaultSrc');
        });

        it('должен вернуть корректный embedType для default html', () => {
            const embedAdapterType = getEmbedAdapterType('default', 'html');
            expect(embedAdapterType).toEqual('default');
        });

        it('должен вернуть корректный embedType для mailru video', () => {
            const embedAdapterType = getEmbedAdapterType('mailru', 'video');
            expect(embedAdapterType).toEqual('defaultSrc');
        });

        it('должен вернуть корректный embedType для vk post', () => {
            const embedAdapterType = getEmbedAdapterType('vk', 'post');
            expect(embedAdapterType).toEqual('vk');
        });

        it('должен вернуть корректный embedType для universal video', () => {
            const embedAdapterType = getEmbedAdapterType('universal', 'video');
            expect(embedAdapterType).toEqual('universal');
        });
    });

    describe('isVersionAtLeast', () => {
        it('версии без точек', () => {
            expect(isVersionAtLeast(10, '9')).toEqual(false);
            expect(isVersionAtLeast(10, '10')).toEqual(true);
            expect(isVersionAtLeast(10, '11')).toEqual(true);
        });

        it('версия os имеет одну точку (минорное обновление)', () => {
            expect(isVersionAtLeast(9, '9.3')).toEqual(true);
            expect(isVersionAtLeast(9, '9.0')).toEqual(true);
            expect(isVersionAtLeast(9.0, '9.0')).toEqual(true);
            expect(isVersionAtLeast(9.0, '9')).toEqual(true);
            expect(isVersionAtLeast(11, '10.3')).toEqual(false);
            expect(isVersionAtLeast(10.2, '10.3')).toEqual(true);
            expect(isVersionAtLeast(10.4, '10.3')).toEqual(false);
            expect(isVersionAtLeast(-1000, '10.3')).toEqual(true);
        });

        it('версия os имеет несколько точек (суперминорное обновление)', () => {
            // заведомо не закладываемся на такой мелкий таргетинг - сравниваем как если бы обрезали до минорной версии
            expect(isVersionAtLeast(0.2, '0.2.1')).toEqual(true);
            expect(isVersionAtLeast(0.2, '0.2.1.2.3.4.5.6.7.8.9.10')).toEqual(true);
            expect(isVersionAtLeast(0.1, '0.2.1')).toEqual(true);
            expect(isVersionAtLeast(0.3, '0.2.1')).toEqual(false);
            expect(isVersionAtLeast(4, '3.4.5')).toEqual(false);
            expect(isVersionAtLeast(3, '3.4.5')).toEqual(true);
            expect(isVersionAtLeast(3, '3.0.0')).toEqual(true);
            expect(isVersionAtLeast(3.1, '3.0.0')).toEqual(false);
            expect(isVersionAtLeast(2, '3.4.5')).toEqual(true);
        });

        it('версия os записана некорректно - не число', () => {
            // при таком кейсе считаем, что лучше покажем контент,
            // а он упадёт у клиента в неблагоприятном сценарии, чем совсем не покажем
            expect(isVersionAtLeast(10, '')).toEqual(true);
            expect(isVersionAtLeast(10, 'Meadow')).toEqual(true);
        });
    });
});
