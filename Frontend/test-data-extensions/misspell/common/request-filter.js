module.exports = {
    type: 'wizard',
    request_text: 'поведение котов | Записки доктора Бегемота begemoten.ru›tag/' +
                  'povedenie-kotov поведение котов. Психология поведения кошек. ' +
                  'Часть 1 – мимика, жесты и позы. Понимать своего кота или кошку ' +
                  'очень важно для хозяина. Поведение и повадки котов - Жизнь ' +
                  'замечательных зверей - Форум... forum.stimka.ru›index.php?/' +
                  'topic/ 30 ноября 2011 Если вы грустны или напряжены' +
                  ', то можете заметить изменение и в',
    data_stub: [
        {
            applicable: 1,
            cause: 'too_many_words',
            counter_prefix: '/wiz/request_filter/',
            length: 53,
            package: 'YxWeb::Wizard::RequestFilter',
            relev: 1.1,
            tail: ' ноября 2011 Если вы грустны или напряжены, то можете заметить ' +
                  'изменение и в',
            text: 'поведение котов | Записки доктора Бегемота begemoten.ru›tag/' +
                  'povedenie-kotov поведение котов. Психология поведения кошек. ' +
                  'Часть 1 – мимика, жесты и позы. Понимать своего кота или кошку ' +
                  'очень важно для хозяина. Поведение и повадки котов - Жизнь ' +
                  'замечательных зверей - Форум... forum.stimka.ru›index.php?/' +
                  'topic/ 30',
            type: 'request_filter',
            types: {
                all: [
                    'wizard',
                    'request_filter'
                ],
                extra: [],
                kind: 'wizard',
                main: 'request_filter'
            },
            wizplace: 'upper'
        },
        {
            applicable: 1,
            counter_prefix: '/wiz/misspell/',
            items: [
                {
                    clear_text: 'поведение котов | Записки доктора Бегемота ' +
                                'begemoten.ru\"tag/povedenie-kotov поведение котов' +
                                '. Психология поведения кошек. Часть 1 - мимика, ' +
                                'жесты и позы. Понимать своего кота или кошку ' +
                                'очень важно для хозяина. Поведение и повадки ' +
                                'котов - Жизнь замечательных зверей - Форум... ' +
                                'forum.stimka.ru\"index.php?/topic/ ' +
                                '30 ноября 2011 Если вы грустны или напряжены, то ' +
                                'можете заметить изменение',
                    dist: '2',
                    flags: 0,
                    force: 0,
                    from: 'report',
                    orig_penalty: '2',
                    raw_source_text: 'поведение котов | Записки доктора Бегемота ' +
                                     'begemoten.ru\"tag/povedenie-kotov поведение ' +
                                     'котов. Психология поведения кошек. Часть 1 - ' +
                                     'мимика, жесты и позы. Понимать своего кота ' +
                                     'или кошку очень важно для хозяина. Поведение ' +
                                     'и повадки котов - Жизнь замечательных зверей' +
                                     ' - Форум... forum.stimka.ru\"index.php?/' +
                                     'topic/ 30 ноября 2011 Если вы ' +
                                     'грустны или напряжены, то можете заметить ' +
                                     'изменение и в ',
                    raw_text: 'поведение котов | Записки доктора Бегемота ' +
                              'begemoten.ru\"tag/povedenie-kotov поведение котов. ' +
                              'Психология поведения кошек. Часть 1 - мимика, ' +
                              'жесты и позы. Понимать своего кота или кошку очень ' +
                              'важно для хозяина. Поведение и повадки котов - ' +
                              'Жизнь замечательных зверей - Форум... ' +
                              'forum.stimka.ru\"index.php?/topic/ ' +
                              '30 ноября 2011 Если вы грустны или напряжены, то ' +
                              'можете заметить изменение и в ',
                    relev: 100,
                    source: 'Misspell',
                    weight: 792
                }
            ],
            orig_weight: 792,
            package: 'YxWeb::Wizard::Misspell::Generic',
            relev: 1.1,
            source: 'report',
            subtype: [
                'misspell'
            ],
            type: 'misspell_source',
            types: {
                all: [
                    'wizard',
                    'misspell'
                ],
                extra: [],
                kind: 'wizard',
                main: 'misspell'
            },
            use_report: 1,
            wizplace: 'upper',
            xml: '<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<MisspellResult ' +
                 'code=\"201\" lang=\"ru,en\" rule=\"Misspell\" flags=\"0\" ' +
                 'r=\"100\"><srcText>поведение котов | Записки доктора Бегемота ' +
                 'begemoten.ru&quot;tag/povedenie-kotov поведение котов. ' +
                 'Психология поведения кошек. Часть 1 - мимика, жесты и позы. ' +
                 'Понимать своего кота или кошку очень важно для хозяина. ' +
                 'Поведение и повадки котов - Жизнь замечательных зверей - ' +
                 'Форум... forum.stimka.ru&quot;index.php?/topic/ ' +
                 '30 ноября 2011 Если вы грустны или напряжены, то можете ' +
                 'заметить изменение и в </srcText><text>поведение котов | ' +
                 'Записки доктора Бегемота begemoten.ru&quot;tag/povedenie-kotov ' +
                 'поведение котов. Психология поведения кошек. Часть 1 - мимика, ' +
                 'жесты и позы. Понимать своего кота или кошку очень важно для ' +
                 'хозяина. Поведение и повадки котов - Жизнь замечательных зверей ' +
                 '- Форум... forum.stimka.ru&quot;index.php?/topic/658.i.kotov/ ' +
                 '30 ноября 2011 Если вы грустны или напряжены, то можете ' +
                 'заметить изменение и в </text><f name=\"weight\" ' +
                 'value=\"791.397413\"/><f name=\"dist\" value=\"2\"/>' +
                 '<f name=\"origWeight\" value=\"791.397413\"/><f name=\"origPenalty\" value=\"2\"/></MisspellResult>'
        }
    ]
};
