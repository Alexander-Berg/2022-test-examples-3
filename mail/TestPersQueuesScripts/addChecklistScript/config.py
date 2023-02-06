# -*- coding: utf-8 -*-

data = {

    "PASSP": {

        "filter": 'Queue: PASSP Components: "@Паспорт" Components: "%Фронтенд" Type: Release Resolution: empty()',

        "data": [
            {
                "text": "Написаны тест-кейсы", "checked": "false"
            },
            {
                "text": "Написаны автотесты", "checked": "false"
            },
            {
                "text": "Проверки безопасности пройдены", "checked": "false"
            },
            {
                "text": "Регресс автотестами пройден", "checked": "false"
            },
            {
                "text": "Регресс асессорами пройден", "checked": "false"
            },
            {
                "text": "Ручной регресс пройден", "checked": "false"
            },
            {
                "text": "Весь релиз протестирован и готов к выкатке", "checked": "false"
            },
        ]
    },

    "MOBDEVAUTH": {

        "filter": 'Queue: MOBDEVAUTH Type: Release Resolution: empty()',

        "data": [
            {
                "text": "Написаны тест-кейсы", "checked": "false"
            },
            {
                "text": "Написаны автотесты", "checked": "false"
            },
            {
                "text": "Регресс автотестами пройден", "checked": "false"
            },
            {
                "text": "Регресс асессорами пройден", "checked": "false"
            },
            {
                "text": "Ручной регресс пройден", "checked": "false"
            },
            {
                "text": "Весь релиз протестирован и готов к выкатке", "checked": "false"
            },
        ]
    },

    "MOBILEMAIL": {

        "filter": 'Queue: MOBILEMAIL Type: Release Status: ! Closed',

        "data": [
            {
                "text": "Проставлено поле QA", "checked": "false"
            },
            {
                "text": "Проставлен Deadline (дата раскатки на 1%)", "checked": "false"
            },
            {
                "text": "Проставлен Release type", "checked": "false"
            },
            {
                "text": "Заполнено поле Release note (ключевая фича)", "checked": "false"
            },
            {
                "text": "Тестирование \"тела\" релиза завершено", "checked": "false"
            },
            {
                "text": "Приступили к первой итерации регресса", "checked": "false"
            },
            {
                "text": "Первая итерация регресса завершена", "checked": "false"
            },
            {
                "text": "Весь релиз протестирован и готов к выкатке", "checked": "false"
            }
        ]
    },

    "MOBDISK": {

        "filter": 'Queue: MOBDISK Type: Release Resolution: empty() "Release Type": ! "Hotfix"',

        "data": [
            {
                "text": "Тестирование \"тела\" релиза завершено", "checked": "false"
            },
            {
                "text": "Приступили к первой итерации регресса", "checked": "false"
            },
            {
                "text": "Первая итерация регресса завершена", "checked": "false"
            },
            {
                "text": "Весь релиз протестирован и готов к выкатке", "checked": "false"
            }
        ]
    }
}
