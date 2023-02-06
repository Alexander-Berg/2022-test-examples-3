import CommandsParser from '../commands';

describe('CommandsParser', (): void => {
    test('parseCommands', async () => {
        const rawData = {
            data: {
                rows: [
                    [
                        {raw: '/dayoff'},
                        {raw: 'Всё про отсутствия на рабочем месте'},
                        {raw: 'Как оформить отсутствие'},
                        {raw: ''},
                        {
                            raw:
                                'Отметь отсутствие в календаре, выбрав желтый квадратик "Отсутствие" и выделив нужный день: https://staff.yandex-team.ru/gap/. Не забудь согласовать это со своим руководителем! ',
                        },
                    ],
                    [
                        {raw: ''},
                        {raw: ''},
                        {raw: 'Как взять отгул'},
                        {raw: ''},
                        {
                            raw:
                                'Подробно про отгул можно почитать тут: https://wiki.yandex-team.ru/HR/KadrovyjjUchet/otgul/',
                        },
                    ],
                    [
                        {raw: '/workschedule'},
                        {raw: 'Всё о режиме работы'},
                        {raw: 'Работа в выходной день'},
                        {raw: ''},
                        {
                            raw:
                                'Информацию об этом можно почитать здесь: https://wiki.yandex-team.ru/hr/kadrovyjjuchet/dayoff/',
                        },
                    ],
                ],
            },
        };

        const parser = new CommandsParser(rawData);
        parser.parse();

        expect((parser as any).command2questions).toEqual({
            '/dayoff': {
                description: 'Всё про отсутствия на рабочем месте',
                questions: ['Как оформить отсутствие', 'Как взять отгул'],
            },
            '/workschedule': {
                description: 'Всё о режиме работы',
                questions: ['Работа в выходной день'],
            },
        });

        expect((parser as any).question2answer).toEqual({
            'Как оформить отсутствие':
                'Отметь отсутствие в календаре, выбрав желтый квадратик "Отсутствие" и выделив нужный день: https://staff.yandex-team.ru/gap/. Не забудь согласовать это со своим руководителем!',
            'Как взять отгул':
                'Подробно про отгул можно почитать тут: https://wiki.yandex-team.ru/HR/KadrovyjjUchet/otgul/',
            'Работа в выходной день':
                'Информацию об этом можно почитать здесь: https://wiki.yandex-team.ru/hr/kadrovyjjuchet/dayoff/',
        });
    });
});
