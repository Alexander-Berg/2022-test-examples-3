import { Config } from '../Config';
import { presets } from '../presets';

describe('#Config', () => {
    it('sould merge flags correctly', () => {
        let config = new Config(
            presets.origin.external.prod,
            {
                flags: { newHeader: '1' },
            },
            {
                flags: { reactions: '1' },
            },
        );

        expect(config.values.flags).toEqual({
            newHeader: '1',
            reactions: '1',
        });

        config = new Config(
            presets.origin.external.prod,
            {
                flags: { reactions: '1' },
            },
            {
                flags: { reactions: '0' },
            },
        );

        expect(config.values.flags).toEqual({
            reactions: '0',
        });
    });

    it('should build default url based on default preset params', () => {
        const YandexConfig = Config.extend(
            presets.backend.prod,
            presets.origin.external.prod,
            presets.type.external.multiChats(),
        );
        const config = new YandexConfig();

        expect(config.getMessengerUrl(
            'test',
            2,
        )).toEqual('https://yandex.ru/chat?config=production&build=chamb&lang=ru&parentOrigin=https%3A%2F%2Fyandex.ru&parentUrl=https%3A%2F%2Fyandex.ru%2F&utm_source=widget&utm_medium=iframe&widgetId=test&flags=newHeader%3D1%3BembedButton%3D1&protocolVersion=2');
    });

    it('should add debug=* to query parameter', () => {
        const YandexConfig = Config.extend(
            presets.backend.prod,
            presets.origin.external.prod,
            presets.type.external.multiChats(),
        );

        const config = new YandexConfig(
            presets.debug.all,
        );

        expect(config.getMessengerUrl(
            'widgetId1',
            2,
        )).toEqual('https://yandex.ru/chat?config=production&build=chamb&lang=ru&debug=*&parentOrigin=https%3A%2F%2Fyandex.ru&parentUrl=https%3A%2F%2Fyandex.ru%2F&utm_source=widget&utm_medium=iframe&widgetId=widgetId1&flags=newHeader%3D1%3BembedButton%3D1&protocolVersion=2');
    });

    it('should add debug=someEvent to query parameter', () => {
        const YandexConfig = Config.extend(
            presets.backend.prod,
            presets.origin.external.prod,
            presets.type.external.multiChats(),
        );

        const config = new YandexConfig(
            presets.debug.filter('someEvent'),
        );

        expect(config.getMessengerUrl(
            'widgetId1',
            2,
        )).toEqual('https://yandex.ru/chat?config=production&build=chamb&lang=ru&debug=someEvent&parentOrigin=https%3A%2F%2Fyandex.ru&parentUrl=https%3A%2F%2Fyandex.ru%2F&utm_source=widget&utm_medium=iframe&widgetId=widgetId1&flags=newHeader%3D1%3BembedButton%3D1&protocolVersion=2');
    });

    it('support preset should return valid data', () => {
        expect(presets.support('GUID')).toMatchObject({
            unreadWithCountChats: true,
            unreadCounterOtherGuid: 'GUID',
            iframeOpenData: {
                guid: 'GUID',
            },
        });
    });
});
