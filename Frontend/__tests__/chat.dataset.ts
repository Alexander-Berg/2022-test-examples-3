export function getChatBar() {
    return {
        img: 'https://jing.yandex-team.ru/files/apanichkina/og_og_1467969417281784822.jpg',
        title: {
            i18n_key: 'yahealth_finish_consultation',
            text: 'Позвони мне позвони, позвони мне ради Бога',
        },
        subtitle: {
            i18n_key: 'yahealth_finish_consultation',
            text: '**Жирненький текст**, а потом не жирный оп оп',
        },
        button: {
            text_color: '#dd9475',
            bg_color: '#000',
            title: {
                i18n_key: 'yahealth_finish_consultation',
                text: '**Позвоните мне**',
            },
            directives: [
                {
                    type: 'client_action',
                    name: 'call_phone',
                    payload: {
                        phone: '123456789',
                    },
                },
            ],
        },
    };
}
