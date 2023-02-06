const { YandexMini2, Color, YandexStationMax, YandexMicro, YandexStationMidi } = require('../../../../speakers');

describe('Сценарии', () => {
    describe('Создание', () => {
        describe('Цветные колонки', () => {
            async function testSpeakers({ browser, PO }, speakers) {
                // Авторизуемся под пользователем из группы 'with-devices'
                await browser.yaLoginWritable();
                // Добавляем устройства пользователю
                await browser.yaAddSpeakers(speakers);

                // - Создание сценария (экран выбора устройства)
                await browser.yaOpenPage('iot/scenario/add/action', '.iot-scenario-edit-action-selector');
                await browser.yaAssertView('plain', 'body');
            }

            it('Станция Макс', async function() {
                await testSpeakers(this, [
                    new YandexStationMax('Станция Макс черная', Color.BLACK),
                    new YandexStationMax('Станция Макс белая', Color.WHITE),
                    new YandexStationMax('Станция Макс синяя', Color.BLUE),
                    new YandexStationMax('Станция Макс красная', Color.RED),
                ]);
            });

            it('Лайт', async function() {
                await testSpeakers(this, [
                    new YandexMicro('Станция Лайт голубая', Color.GREEN),
                    new YandexMicro('Станция Лайт пурпурная', Color.PURPLE),
                    new YandexMicro('Станция Лайт красная', Color.RED),
                    new YandexMicro('Станция Лайт бежевая', Color.BEIGE),
                    new YandexMicro('Станция Лайт желтая', Color.YELLOW),
                    new YandexMicro('Станция Лайт розовая', Color.PINK),
                ]);
            });

            it('Мини 2', async function() {
                await testSpeakers(this, [
                    new YandexMini2('Станция Мини 2 черная', Color.BLACK),
                    new YandexMini2('Станция Мини 2 серая', Color.GRAY),
                    new YandexMini2('Станция Мини 2 синяя', Color.BLUE),
                    new YandexMini2('Станция Мини 2 красная', Color.RED),
                ]);
            });

            it('Миди', async function() {
                await testSpeakers(this, [
                    new YandexStationMidi('Станция Миди черная', Color.BLACK),
                    new YandexStationMidi('Станция Миди бежевая', Color.BEIGE),
                    new YandexStationMidi('Станция Миди синяя', Color.BLUE),
                    new YandexStationMidi('Станция Миди красная', Color.COPPER),
                ]);
            });
        });
    });
});
