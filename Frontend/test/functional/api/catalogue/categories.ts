/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { callApi, respondsWithResult } from './_helpers';
import { wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

// Тест для нового списка категорий
test('get categories', async t => {
    const res = await callApi('get', '/categories/');
    respondsWithResult(
        {
            result: [
                {
                    type: 'smart_home',
                    title: 'Умный дом Яндекса',
                    color: '#26B883',
                    emoji: '🏡',
                },
                {
                    type: 'games_trivia_accessories',
                    title: 'Игры и развлечения',
                    color: '#35E874',
                    emoji: '🎮',
                },
                {
                    type: 'kids',
                    title: 'Для детей',
                    color: '#FA5083',
                    emoji: '🦄',
                },
                {
                    type: 'shopping',
                    title: 'Покупки',
                    color: '#7C50FA',
                    emoji: '🛍',
                },
                {
                    type: 'food_drink',
                    title: 'Еда и напитки',
                    color: '#F45351',
                    emoji: '🍎',
                },
                {
                    type: 'education_reference',
                    title: 'Образование',
                    color: '#5069FA',
                    emoji: '📚',
                },
                {
                    type: 'news',
                    title: 'Новости',
                    color: '#50BDFA',
                    emoji: '📰',
                },
                {
                    type: 'communication',
                    title: 'Общение',
                    color: '#FAB650',
                    emoji: '👄',
                },
                {
                    type: 'health_fitness',
                    title: 'Спорт и здоровье',
                    color: '#26B1FF',
                    emoji: '🏓',
                },
                {
                    type: 'travel_transportation',
                    title: 'Путешествия',
                    color: '#FAA250',
                    emoji: '🌅',
                },
                {
                    type: 'business_finance',
                    title: 'Бизнес и финансы',
                    color: '#F54177',
                    emoji: '💸',
                },
                {
                    type: 'productivity',
                    title: 'Продуктивность',
                    color: '#506BFA',
                    emoji: '📒',
                },
                {
                    type: 'utilities',
                    title: 'Управление',
                    color: '#EA844A',
                    emoji: '⚙️',
                },
                {
                    type: 'local',
                    title: 'Поиск и быстрые ответы',
                    color: '#97AD58',
                    emoji: '🔍',
                },
                {
                    type: 'weather',
                    title: 'Погода',
                    color: '#C496ED',
                    emoji: '☔️',
                },
                {
                    type: 'connected_car',
                    title: 'Авто',
                    color: '#60ABE0',
                    emoji: '🚙',
                },
                {
                    type: 'movies_tv',
                    title: 'Видео',
                    color: '#FA6F50',
                    emoji: '🎥',
                },
                {
                    type: 'music_audio',
                    title: 'Аудио и подкасты',
                    color: '#FA50B6',
                    emoji: '🎙',
                },
                {
                    type: 'lifestyle',
                    title: 'Культура',
                    color: '#FAAC50',
                    emoji: '🏺',
                },
            ],
        },
        res,
        t,
    );
});
