var testData = {
    userJson: {
        '': {
            settings: {
                first_login: true
            }
        },
		'has:liza exp:new-avatars': {
			"currentInterface": "liza",
			"supportedInterfaces": [ "liza", "lite" ],
			"isCorp": false,
			"isPDD": false,
			"isWS": false,
			"settings": {
				"color_scheme": "cosmos",
				"exp-53505": "on"
			}
		},
        'has:liza not:corp lang:ru': {
            "currentInterface": "liza",
            "daysFromRegistration": 190,
            "isCorp": false,
            "isPDD": false,
            "isWS": false,
            "settings": {},
            "locale": "ru"
        },
        'has:liza not:corp': {
            "currentInterface": "liza",
            "supportedInterfaces": ["daria", "lite", "liza"],
            "daysFromRegistration": 868.9292911342593,
            "isCorp": false,
            "isPDD": false,
            "isWS": false,
            "settings": {}
        },

        'has:daria good-for:liza': {
            "currentInterface": "daria",
            "supportedInterfaces": ["daria", "lite", "liza"],
            "daysFromRegistration": 868.9292911342593,
            "isCorp": false,
            "isPDD": false,
            "isWS": false,
            "settings": {
                "collectors_promo_s": "1461775470122",
                "exp": "2",
                "exp-24088": "on",
                "exp-24088-applied": "on",
                "last-login-ts": "1475566169004",
                "last_news": "24",
                "localize_imap": "on",
                "messages_avatars": "on",
                "no_collect_bubbl_all": "on",
                "no_collectors_bubble": "on",
                "no_collectors_promo": "on",
                "promo-manager": "%7B%22last-time-show-promo%22%3A1475576669059%2C%22last-promo-was-name%22%3A%22yabrowser-promoline%22%7D",
                "rph": "mozilla=0&opera=0&webkit=1",
                "search-version": "2014.03.14",
                "abook_page_size": "50",
                "collect_addresses": true,
                "color_scheme": "colorful",
                "default_email": "yndx.chestozo@yandex.ru",
                "default_mailbox": "yandex.ru",
                "dnd_enabled": true,
                "enable_autosave": true,
                "enable_firstline": true,
                "enable_hotkeys": "on",
                "enable_imap": true,
                "enable_pop": false,
                "enable_quoting": true,
                "enable_richedit": true,
                "enable_social_notification": true,
                "first_login": false,
                "folder_thread_view": true,
                "from_name": "Роман Карцев",
                "hide_daria_header": false,
                "jump_to_next_message": false,
                "label_sort": "by_count",
                "messages_per_page": "30",
                "page_after_delete": "current_list",
                "page_after_move": "current_list",
                "page_after_send": "done",
                "pop3_archivate": "on",
                "pop3_makes_read": false,
                "pop_spam_subject_mark_enable": true,
                "save_sent": true,
                "show_advertisement": true,
                "show_avatars": true,
                "show_news": true,
                "show_socnet_avatars": false,
                "show_todo": false,
                "signature_eng": "",
                "signature_top": false,
                "translate": true,
                "use_monospace_in_text": false,
                "signs": [],
                "emails": [{
                    "native": true,
                    "validated": true,
                    "def": false,
                    "rpop": false,
                    "address": "yndx.chestozo@ya.ru",
                    "date": "2014-05-19 16:09:56"
                }, {
                    "native": true,
                    "validated": true,
                    "def": false,
                    "rpop": false,
                    "address": "yndx.chestozo@yandex.by",
                    "date": "2014-05-19 16:09:56"
                }, {
                    "native": true,
                    "validated": true,
                    "def": false,
                    "rpop": false,
                    "address": "yndx.chestozo@yandex.com",
                    "date": "2014-05-19 16:09:56"
                }, {
                    "native": true,
                    "validated": true,
                    "def": false,
                    "rpop": false,
                    "address": "yndx.chestozo@yandex.kz",
                    "date": "2014-05-19 16:09:56"
                }, {
                    "native": true,
                    "validated": true,
                    "def": true,
                    "rpop": false,
                    "address": "yndx.chestozo@yandex.ru",
                    "date": "2014-05-19 16:09:56"
                }, {
                    "native": true,
                    "validated": true,
                    "def": false,
                    "rpop": false,
                    "address": "yndx.chestozo@yandex.ua",
                    "date": "2014-05-19 16:09:56"
                }],
                "reply_to": [],
                "signature": "",
                "sanitize_signature": false
            }
        },

        'has:exp:38033': {
            "settings": {
                "exp-38033": "on"
            }
        }
    },

    expCustomJson: {
        '': {
            "conditions": "'first_login' of settings == 'true'",
            "actions": {
                "settings": {}
            }
        },

        // https://ab.yandex-team.ru/testid/32334
        '[32334] Переключалка в Лизу': {
            "conditions": "'liza' in supportedInterfaces && currentInterface == 'daria' && !isCorp && 'color_scheme' of settings not in ['galatasaray', 'fenerbahce', 'besiktas']",
            "actions": {
                "settings": {
                    "u2709": "on",
                    "disable-neo2": true,
                    "show_checkbox_inside_userpic": false
                }
            }
        },

        '[32554] Реклама в неактивной вкладке обновляется': {
            "conditions": "currentInterface == 'liza' && !isCorp",
        },

        'Проверка наличия у пользователя эксперимента 38033': {
            "conditions": "'exp-38033' of settings == 'on'",
            "actions": {
                "settings": {}
            }
        }
    }
};
