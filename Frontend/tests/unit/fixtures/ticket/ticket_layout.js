module.exports = {
    create: Object.assign({
        title: 'test ticket',
        description: 'test description',
    }, getGeneral()),

    update: Object.assign({
        title: 'updated ticket',
        description: 'updated description',
    }, getGeneral()),
};

function getGeneral() {
    return {
        goodTasks: 5,
        badTasks: 1,
        pool: {
            name: 'touch_320',
            poolId: 195443,
            sandboxId: 8936,
        },
        layouts: {
            systems: [
                { name: 'Синяя тема' },
                { name: 'Зеленая тема' },
                { name: 'Черная тема' },
            ],
            screens: [
                {
                    name: 'Страница авторизации',
                    question: 'Как вам страница авторизации?',
                },
                {
                    name: 'Страница треда',
                    question: 'Как вам страница треда?',
                },
            ],
            layouts: [
                {
                    screens: [
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb1.png' },
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb2.png' },
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb3.png' },
                    ],
                    honeypots: [
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb4.png' },
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb5.png' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb6.png' },
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb7.png' },
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb8.png' },
                    ],
                    honeypots: [
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb9.png' },
                        { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb10.png' },
                    ],
                },
            ],
        },
    };
}
