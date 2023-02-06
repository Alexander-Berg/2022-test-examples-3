# language: ru
@not_stand
Функция: обратный поиск
    Как пользователь поиска
    Чтобы находить ответы на запросы вида "Что здесь?" и "Какие организации находятся в этом доме?"
    Я хочу, чтобы поиск поддерживал обратный поиск

    Предыстория:
        Пусть выбрана локаль ru_RU

    Структура сценария: обратное геокодирование дома
        Пусть запрос на обратное геокодирование содержит координаты "<coord>"
        И тип обратного геокодирования "geo"
        Когда пользователь запускает обратное геокодирование
        Тогда он получает непустой ответ
        И на первом месте находится объект с именем "<toponym_name>"
        И на первом месте находится топоним с типом "house"

        Примеры:
            | coord               | toponym_name            |
            | 37.587051,55.734059 | улица Льва Толстого, 16 |
            | 38.236609,55.567108 | улица Гурьева, 11А      |
            | 30.327710,59.935575 | Невский проспект, 30    |

    Структура сценария: обратное геокодирование топонима
        Пусть запрос на обратное геокодирование содержит координаты "<coord>"
        И тип обратного геокодирования "geo"
        Когда пользователь запускает обратное геокодирование
        Тогда он получает непустой ответ
        И на первом месте находится объект с именем "<toponym_name>"

        Примеры:
            | coord               | toponym_name               |
            | 37.587230,55.733590 | улица Льва Толстого        |
            | 37.652094,55.661319 | парк имени Юрия Лужкова    |
            | 37.686864,55.295495 | городской округ Домодедово |
            | 37.433506,55.790068 | Чистый залив               |
            | 30.330363,59.934825 | Невский проспект           |

    Структура сценария: поиск организации в "этом доме"
        Пусть запрос на обратное геокодирование содержит координаты "<coord>"
        И тип обратного геокодирования "biz"
        И пользователь запрашивает 40 результатов
        Когда пользователь запускает обратное геокодирование
        Тогда он получает непустой ответ
        И все организации в ответе находятся на расстоянии не более 300 метров от точки "<coord>"
        И все организации в ответе имеют точность геокодирования "EXACT"
        И в ответе есть организация с именем "<name>"

        Примеры:
            | coord               | name                                                          |
            | 37.587924,55.733809 | Яндекс                                                        |
            | 37.531002,55.702971 | Московский государственный университет имени М. В. Ломоносова |
            | 30.406108,59.959219 | Яндекс                                                        |

    Структура сценария: обратное геокодирование по тексту запроса
        Пусть запрос пользователя равен "<user_query>"
        И запрос задаётся откуда-то из региона "Москва"
        Когда пользователь запускает поиск
        Тогда он получает непустой ответ
        И на первом месте находится объект с именем "<toponym_name>"

        Примеры:
            | user_query           | toponym_name               |
            | 55.734059,37.587051  | улица Льва Толстого, 16    |
            | 55.295495 37.686864  | городской округ Домодедово |
            | 55.567108, 38.236609 | улица Гурьева, 11А         |