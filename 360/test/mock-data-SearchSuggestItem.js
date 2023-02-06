const longOne = ' Creates an array of elements split into groups the length of size. If array can\'t be split ' +
    'evenly, the final chunk will be the remaining elements.';

export const suggestItems = [
    {
        target: 'history',
        show_text: '\u003cscript\u003ealert(\'hi\')\u003c/script\u003e',
        search_text: '\u003cscript\u003ealert(\'hi\')\u003c/script\u003e',
        // eslint-disable-next-line max-len
        show_text_highlighted: '\u003cspan class=\"msearch-highlight\"\u003e\u003cscript\u003ealert(\'hi\')\u003c/script\u003e\u003c/span\u003e'
    },
    {
        search_text: 'Привет дружок' + longOne,
        show_text: 'Привет дружок' + longOne,
        show_text_highlighted: 'Привет дружок' + longOne,
        target: 'history'
    },
    {
        search_text: 'привет' + longOne,
        show_text: 'привет' + longOne,
        show_text_highlighted: '<span class="msearch-highlight">при</span>вет' + longOne,
        target: 'subject'
    },
    {
        lid: '139',
        search_text: 'метка:tru' + longOne,
        show_text: 'tru' + longOne,
        show_text_highlighted: '<span class="msearch-highlight">tru ' + longOne + '</span>' + longOne,
        target: 'label'
    },
    {
        search_text: 'папка:Входящие' + longOne,
        show_text: 'Входящие' + longOne,
        show_text_highlighted: '<span class="msearch-highlight">Входящие</span>' + longOne,
        target: 'folder'
    },
    {
        search_text: 'attachment:' + longOne,
        show_text: 'attachment:' + longOne,
        target: 'ql'
    },
    {
        avatarRef: '5ce75b7bf52471ead572f8d9f2075b84',
        display_name: 'jøran' + longOne,
        display_name_highlighted: 'jøran' + longOne,
        email: 'jøran@blåbærsyltetøy.gulbrandsen.priv.no' + longOne,
        email_highlighted: 'jøran@blåbærsyltetøy.gulbrandsen.<span class="msearch-highlight">pri</span>v.no' + longOne,
        search_text: 'jøran@blåbærsyltetøy.gulbrandsen.priv.no' + longOne,
        show_text: '"jøran" jøran@blåbærsyltetøy.gulbrandsen.priv.no' + longOne,
        target: 'contact',
        unread_cnt: 0
    },
    {
        avatarRef: '1508565ba1386ae3a1674966d92f252b',
        display_name: 'Карцев Роман' + longOne,
        email: 'chestozo@yandex.ru' + longOne,
        has_attachments: false,
        mid: '161848111608629900',
        received_date: '1492779658',
        search_text: 'mid:161848111608629900' + longOne,
        show_text: 'No subject' + longOne,
        show_text_highlighted: 'No subject' + longOne,
        target: 'mail'
    }
];
