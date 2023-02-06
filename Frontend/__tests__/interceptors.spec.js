const fs = require('fs');
const path = require('path');
const faker = require('faker');

const {
    wsInterceptClientMsg,
    wsInterceptServerMsg,
    mapOldMessageIdToNew,
    mapOldRequestIdToNew,
} = require('../interceptors');

const loggerMock = {
    debug: jest.fn(),
    error: jest.fn(),
};

const readModeOptions = { req: { kotik: { setup: { params: { cacheMode: 'read' } }, logger: loggerMock } } };
const writeModeOptions = { req: { kotik: { setup: { params: { cacheMode: 'write' } }, logger: loggerMock } } };

const websocketData = JSON.parse(fs.readFileSync(path.join(__dirname, 'dataset/websocket.json'), 'utf-8'));

const cleanInterceptorsMap = () => {
    // Очищаем объект соответствий перед каждым тестом
    Object.keys(mapOldMessageIdToNew).map((key) => {
        delete mapOldMessageIdToNew[key];
    });
    Object.keys(mapOldRequestIdToNew).map((key) => {
        delete mapOldRequestIdToNew[key];
    });
};

describe('kotik/ws-proxy/interceptors', () => {
    beforeEach(cleanInterceptorsMap);

    it.skip('should correctly replace RequestId', () => {
        const mapNewRequestIdToOld = {};

        websocketData.messages.forEach((frame) => {
            if (frame.type === 'request') {
                const parsedData = JSON.parse(frame.data);
                const parsedDumpData = JSON.parse(frame.data);

                if (
                    parsedData.event.payload.ClientMessage &&
                    parsedData.event.payload.ClientMessage.Plain &&
                    parsedData.event.payload.ClientMessage.Plain.PayloadId
                ) {
                    /*
                    * Подменяем RequestId в сообщении от клиента
                    * Тем самым эмитируем работу мессенджера, который генерирует RequestId на каждое сообщение
                    */
                    const newRequestId = faker.random.uuid();
                    mapNewRequestIdToOld[newRequestId] = parsedData.event.payload.ClientMessage.Plain.PayloadId;
                    parsedData.event.payload.ClientMessage.Plain.PayloadId = newRequestId;
                }

                wsInterceptClientMsg(
                    JSON.stringify(parsedData),
                    { ...readModeOptions, dumpData: JSON.stringify(parsedDumpData) },
                );
            } else {
                const serverInterceptorResult = wsInterceptServerMsg(frame.data, readModeOptions);

                const parsedData = JSON.parse(serverInterceptorResult);

                // Проверяем, что полученный в зеркальном сообщении RequestId был сохранён
                if (
                    parsedData.directive.payload.ServerMessage &&
                    parsedData.directive.payload.ServerMessage.ClientMessage &&
                    parsedData.directive.payload.ServerMessage.ClientMessage.Plain &&
                    parsedData.directive.payload.ServerMessage.ClientMessage.Plain.PayloadId
                ) {
                    const payloadId = parsedData.directive.payload.ServerMessage.ClientMessage.Plain.PayloadId;

                    console.log('======================', mapNewRequestIdToOld);

                    expect(mapNewRequestIdToOld[payloadId]).toBeTruthy();

                    // Удаляем уже использованное соответствие
                    delete mapNewRequestIdToOld[payloadId];
                }
            }
        });
    });

    describe('wsInterceptClientMsg', () => {
        beforeEach(cleanInterceptorsMap);

        it('should return data for correct JSON args and save messageId match in read mode', () => {
            const clientData = JSON.parse(websocketData.messages[0].data);
            const dumpData = JSON.parse(websocketData.messages[0].data);

            const newUUID = faker.random.uuid();
            clientData.event.header.messageId = newUUID;

            const clientInterceptorResult = wsInterceptClientMsg(
                JSON.stringify(clientData),
                { ...readModeOptions, dumpData: JSON.stringify(dumpData) },
            );

            // верно соответствие ID в дампе ID и нового ID
            expect(mapOldMessageIdToNew[dumpData.event.header.messageId]).toEqual(newUUID);
            // перехватчик возвращает то, что принял
            expect(clientInterceptorResult).toEqual(JSON.stringify(clientData));
        });

        it('should return data for correct first JSON arg and NOT save messageId match in read mode', () => {
            const clientData = JSON.parse(websocketData.messages[0].data);
            const dumpData = JSON.parse(websocketData.messages[0].data);

            const newUUID = faker.random.uuid();
            clientData.event.header.messageId = newUUID;

            const clientInterceptorResult = wsInterceptClientMsg(JSON.stringify(clientData), { ...readModeOptions });
            // нет соответствия для нового messageId
            expect(Object.values(mapOldMessageIdToNew).includes(dumpData.event.header.messageId)).toBeFalsy();
            // перехватчик возвращает то, что принял
            expect(clientInterceptorResult).toEqual(JSON.stringify(clientData));
        });

        it('should return null for not valid JSON data', () => {
            const clientInterceptorResult = wsInterceptClientMsg('not json data', { ...readModeOptions });
            expect(clientInterceptorResult).toBeNull();
        });

        it('should return data for correct JSON args and NOT save messageId match in write mode', () => {
            const clientData = JSON.parse(websocketData.messages[0].data);
            const dumpData = JSON.parse(websocketData.messages[0].data);

            const newUUID = faker.random.uuid();
            clientData.event.header.messageId = newUUID;

            const clientInterceptorResult = wsInterceptClientMsg(
                JSON.stringify(clientData),
                { ...writeModeOptions, dumpData: JSON.stringify(dumpData) },
            );

            // нет соответствия для нового messageId
            expect(Object.values(mapOldMessageIdToNew).includes(clientData.event.header.messageId)).toBeFalsy();
            // перехватчик возвращает то, что принял
            expect(clientInterceptorResult).toEqual(JSON.stringify(clientData));
        });
    });

    describe('wsInterceptServerMsg', () => {
        beforeEach(cleanInterceptorsMap);

        it('should return data with replaced refMessageId if replacements exists in read mode', () => {
            const serverData = JSON.parse(websocketData.messages[1].data);
            const newUUID = faker.random.uuid();
            mapOldMessageIdToNew[serverData.directive.header.refMessageId] = newUUID;
            const serverInterceptorResult = JSON.parse(
                wsInterceptServerMsg(websocketData.messages[1].data, readModeOptions),
            );

            expect(serverInterceptorResult.directive.header.refMessageId).toBe(newUUID);
        });

        it('should return data as is in write mode', () => {
            expect(wsInterceptServerMsg(websocketData.messages[1].data, writeModeOptions))
                .toBe(websocketData.messages[1].data);
        });

        it('should return null if first arguments has not valid json', () => {
            expect(wsInterceptServerMsg('not valid json', writeModeOptions)).toBeNull();
        });
    });
});
