import * as Telegram from 'telegraf/telegram';
import * as sinon from 'sinon';
import {DateTime} from 'luxon';

import Quest from '@/model/quest';
import Image from '@/model/image';

const questData = [
    {
        day: 1,
        question: {
            text:
                'Привет! Сегодня твой первый день в Яндекс.Маркете, а я - *HRoom*, твой интерактивный помощник. Мне хочется, чтобы этот день прошел для тебя максимально интересно и эффективно! Ты уже познакомился со своей командой?',
            images: [],
        },
        answers2reactions: {
            'Да, как иначе!': {
                text: 'Отлично, уверен, что коллеги встретили тебя радушно.',
                images: [],
            },
            'Еще нет, но планирую это сделать': {
                text:
                    'Не стесняйся, подойди к коллегам сам и представься, расскажи немного о себе и это позволит вам быстрее познакомиться друг с другом!',
                images: [],
            },
            'Нет, мне страшно, заберите меня обратно': {
                text:
                    'Яндекс.Маркет - компания близких по духу людей! Тебе нечего бояться, здесь тебе по-настоящему рады, просто попроси своего руководителя представить тебя команде. file:/Market/marketlife/HR/Chat-bot/2018-11-141555.png',
                images: [],
            },
        },
        defaultReaction: {
            text:
                'Не стесняйся, подойди к коллегам сам и представься, расскажи немного о себе и это позволит вам быстрее познакомиться друг с другом!',
            images: [],
        },
    },
    {
        day: 1,
        question: {
            text: 'Я хочу рассказать тебе пару слов о том, как мы будем с тобой взаимодействовать',
            images: [],
        },
        answers2reactions: {
            'Конечно!': {text: 'Список команд HRoom (будут пополняться!):', images: []},
            'Расскажи, но мне будет грустно если ты совсем перестанешь потом мне писать': {
                text: '',
                images: [],
            },
        },
        defaultReaction: {
            text:
                'Список команд HRoom (будут пополняться!):\n\n/dayoff - Есть необходимость отсутствовать на рабочем месте в какой-то день\n\n/workweekend - Есть бизнес-необходимость выйти на работу в выходной день\n\n/facilities - Трудности с заказом оборудования/предмета мебели/растения\n\n/businesstrip - Всё о командировках\n\n/conference - Про конференции и как на них поехать\n\n/salary - Всё о зарплате и её начислении\n\n/badge - Про бейдж\n\n/trainings - Всё о тренингах в компании\n\n/hrdocs - Заказ разных-разнообразных справок\n\n/vacation - Все об отпуске',
            images: [],
        },
    },
    {
        day: 2,
        question: {
            text: 'Хэй! Тебе удалось настроить оборудование или есть какие-то сложности?',
            images: [],
        },
        answers2reactions: {
            'Я со всем разобрался, все отлично!': {
                text:
                    'Супер, я верил в тебя! Если будут какие-то сложности, можешь написать на help@yandex-team, позвонить на номер 444 или дойти до коллег ногами: это третий подъезд БЦ Морозов, 5 этаж (дверь с надписью Helpdesk). Зайдя туда, необходимо приложить бейдж к "пикалке" :)',
                images: [],
            },
            'Да. есть кое-какие проблемы, поможешь?': {
                text:
                    'С этим тебе поможет Helpdesk. Ты можешь написать на help@yandex-team, позвонить на номер 444 или дойти до коллег ногами: это третий подъезд БЦ Морозов, 5 этаж (дверь с надписью Helpdesk). Если ты уже сделал это - значит, твоя заявка в работе, и в скором времени тебе ответят.',
                images: [],
            },
        },
        defaultReaction: {
            text:
                'Если будут какие-то сложности, можешь написать на help@yandex-team, позвонить на номер 444 или дойти до коллег ногами: это третий подъезд БЦ Морозов, 5 этаж (дверь с надписью Helpdesk). Зайдя туда, необходимо приложить бейдж к "пикалке" :)',
            images: [],
        },
    },
];

describe('Quest', () => {
    test('sendMessage with just text', async () => {
        const sendMessageFake = sinon.stub(Telegram.prototype, 'sendMessage');
        sendMessageFake.resolves();

        const quest = new Quest({userUid: 111});

        await (quest as any).sendMessage({
            text: 'hello world',
        });

        expect(sendMessageFake.calledWith(111, 'hello world')).toBe(true);
        sendMessageFake.restore();
    });

    test('sendMessage text with photo', async () => {
        const sendMessageFake = sinon.stub(Telegram.prototype, 'sendMessage');
        sendMessageFake.resolves();

        const sendPhotoFake = sinon.stub(Telegram.prototype, 'sendPhoto');
        sendPhotoFake.resolves();

        const getImageFake = sinon.stub(Image, 'getImage');
        getImageFake.resolves('data');

        const quest = new Quest({userUid: 111});

        await (quest as any).sendMessage({
            text: 'hello world',
            images: ['image1'],
        });

        expect(sendMessageFake.calledWith(111, 'hello world')).toBe(true);
        expect(sendPhotoFake.calledWith(111, {source: 'data'})).toBe(true);
        expect(getImageFake.calledWith('image1')).toBe(true);

        sendMessageFake.restore();
    });

    test('sendNextQuestion', async () => {
        const quest = new Quest({
            userUid: 111,
            stepId: 1,
        });

        const saveFake = sinon.stub(Quest.prototype, 'save');
        saveFake.resolves();

        const getNextQuestionTimeFake = sinon.stub(Quest.prototype, 'getNextQuestionTime');
        const d1 = new Date();
        getNextQuestionTimeFake.returns(d1);

        const toJSDateFake = sinon.stub(DateTime.prototype, 'toJSDate');
        const d2 = new Date();
        toJSDateFake.returns(d2);

        const sendMessageFake = sinon.stub(Telegram.prototype, 'sendMessage');
        sendMessageFake.resolves();

        (quest as any).questContent = questData;
        await quest.sendNextQuestion(null);

        expect(quest.stepId).toBe(2);
        expect(saveFake.calledOnce).toBe(true);

        expect(toJSDateFake.calledOnce).toBe(true);
        expect(quest.waitForAnswerUntil).toEqual(d2);
        expect(quest.stepResolved).toBe(false);
        expect(quest.toNextStepAfter).toBe(d1);
        expect(getNextQuestionTimeFake.calledOnce).toBe(true);
        expect(
            sendMessageFake.calledWith(
                111,
                'Я хочу рассказать тебе пару слов о том, как мы будем с тобой взаимодействовать',
            ),
        ).toBe(true);

        saveFake.restore();
        getNextQuestionTimeFake.restore();
        toJSDateFake.restore();
        sendMessageFake.restore();
    });
});
