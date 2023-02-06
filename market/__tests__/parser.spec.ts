/* eslint-disable class-methods-use-this */

import * as sinon from 'sinon';
import axios from 'axios';

import Parser from '../parser';

// поскольку Parser-абстрактный класс, создаем вспомогательный класс для тестирования
class ChildParser extends Parser {
    protected URL = 'custom url';
    public parse() {
        return [];
    }
    public getData() {
        return null;
    }
}

describe('Parser', (): void => {
    test('fetchData', async () => {
        const fakeGet = sinon.stub(axios, 'get');
        fakeGet.resolves({data: {fake: true}});
        const parser = new ChildParser();
        await parser.fetchData();
        expect((parser as any).rawData).toEqual({fake: true});
    });

    test('convertLinks2Nda - одна ссылка в тексте', async () => {
        const parser = new ChildParser();
        const link2NdaLinkStub = sinon.stub(parser, 'link2NdaLink');
        link2NdaLinkStub.resolves('https://nda/aaa');

        const text =
            'Информацию об этом можно почитать здесь: https://wiki.yandex-team.ru/hr/kadrovyjjuchet/dayoff/';
        const convertedText = await (parser as any).convertLinks2Nda(text);
        expect(convertedText).toBe('Информацию об этом можно почитать здесь: https://nda/aaa');
        link2NdaLinkStub.restore();
    });

    test('convertLinks2Nda - несколько ссылок в тексте', async () => {
        const parser = new ChildParser();
        const link2NdaLinkStub = sinon.stub(parser, 'link2NdaLink');
        link2NdaLinkStub.resolves('https://nda/aaa');

        const text = `Возле офиса Яндекса есть огромное количество ресторанов и заведений, в которых можно вкусно и разнообразно поесть, и главное - там принимают к оплате яндексовые бейджи.
Полный список локаций можно посмотреть на карте (его сосьтавил один добрый яндексоид): https://yandex.ru/maps/213/moscow/?ll=37.590103%2C55.738710&mode=usermaps&source=constructorLink&um=constructor%3A09920ab5e4256b99a9a592bb4ba452b9b7a33513636a3e60934e38f5db967467&z=15.
А вот список заведений, где вкусные завтраки в районе Красной розы: https://wiki.yandex-team.ru/HR/Kompensacii/Novaja-stranica-po-pitaniju/Zavtraki/Zavtraki-Krasnaja-roza/
И остальные кафе, не только про завтраки: https://wiki.yandex-team.ru/HR/Kompensacii/Novaja-stranica-po-pitaniju/Список-/Spisok-tochek-pitanija/`;

        const convertedText = await (parser as any).convertLinks2Nda(text);
        expect(convertedText).toBe(
            `Возле офиса Яндекса есть огромное количество ресторанов и заведений, в которых можно вкусно и разнообразно поесть, и главное - там принимают к оплате яндексовые бейджи.
Полный список локаций можно посмотреть на карте (его сосьтавил один добрый яндексоид): https://nda/aaa.
А вот список заведений, где вкусные завтраки в районе Красной розы: https://nda/aaa
И остальные кафе, не только про завтраки: https://nda/aaa`,
        );

        link2NdaLinkStub.restore();
    });

    test('convertLinks2Nda - markdown-разметка', async () => {
        const parser = new ChildParser();
        const link2NdaLinkStub = sinon.stub(parser, 'link2NdaLink');
        link2NdaLinkStub.resolves('https://nda/aaa');

        const text = `Смотри, здесь находится список из всех [рассылок компании Яндекс] (https://ml.yandex-team.ru/), ты можешь выбрать и другие рассылки и подписаться самостоятельно!`;

        const convertedText = await (parser as any).convertLinks2Nda(text);
        expect(convertedText).toBe(
            `Смотри, здесь находится список из всех [рассылок компании Яндекс] (https://nda/aaa), ты можешь выбрать и другие рассылки и подписаться самостоятельно!`,
        );

        link2NdaLinkStub.restore();
    });
});
