before(function() {

    window.mock['filters'] = [
        {
            "params": {},
            "data": {
                "action": [
                    {
                        "name": "Письма из ящика aandrosov@gmail.com",
                        "priority": 1,
                        "stop": false,
                        "enabled": true,
                        "created": 1267521786,
                        "actions": [{"type": "move", "parameter": "2170000240000008620", "verified": true}],
                        "filid": "2170000000000001875",
                        "condition": [
                            {"div": "X-yandex-rpop-id", "pattern": "20000000017", "oper": 3, "link": "0"},
                            {"div": "nospam", "oper": 1, "link": "1"}
                        ]
                    },
                    {
                        "name": "Моё правило",
                        "priority": 10000,
                        "stop": false,
                        "enabled": true,
                        "created": 1292266966,
                        "actions": [{"type": "movel", "parameter": "2170000000004288303", "verified": true}],
                        "filid": "2170000000000004111",
                        "condition": [
                            {"div": "from", "pattern": "@raiffeisen.ru", "oper": 3, "link": "1"},
                            {"div": "nospam", "oper": 1, "link": "1"}
                        ]
                    }
                ],
                "info": {
                    "action": [{"id": "delete", "label": "Удалить"}, {
                        "id": "movel",
                        "parameter": "noselect",
                        "label": "Пометить прочитанным"
                    }, {"id": "move", "label": "Положить в папку"}, {"id": "movel", "label": "Поставить метку"}, {
                        "id": "forward",
                        "req-pass": "yes",
                        "label": "Переслать по адресу"
                    }, {"id": "notify", "req-pass": "yes", "label": "Уведомить по адресу"}, {
                        "id": "reply",
                        "req-pass": "yes",
                        "label": "Ответить следующим текстом"
                    }],
                    "and-or": [{"value": "0", "label": "выполняется хотя бы одно из условий"}, {
                        "value": "1",
                        "label": "выполняются все условия одновременно"
                    }],
                    "attachment": [{"value": "", "label": "с вложениями и без вложений"}, {
                        "value": "1",
                        "label": "с вложениями"
                    }, {"value": "2", "label": "без вложений"}],
                    "conditions": [{"value": "1", "label": "совпадает c"}, {"value": "2", "label": "не совпадает c"}, {
                        "value": "3",
                        "label": "содержит"
                    }, {"value": "4", "label": "не содержит"}],
                    "fields": [{"value": "from", "label": "От кого"}, {
                        "value": "to-or-cc",
                        "label": "Кому или копия"
                    }, {"value": "to", "label": "Кому"}, {"value": "cc", "label": "Копия"}, {
                        "value": "subject",
                        "label": "Тема"
                    }, {"value": "body", "label": "Тело письма"}, {
                        "value": "filename",
                        "label": "Имя вложения"
                    }, {"value": "sender", "label": "Заголовок"}],
                    "logic": [{"value": "nospam", "label": "ко всем письмам, кроме спама"}, {
                        "value": "all",
                        "label": "ко всем письмам, включая спам"
                    }, {"value": "clearspam", "label": "только к спаму"}]
                }
            }
        }
    ];

});
