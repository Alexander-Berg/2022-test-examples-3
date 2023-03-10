# language: ru
@collections_search
Функция: поиск по коллекциям

    Структура сценария: поиск коллекций
        Пусть для поиска доступна только вертикаль коллекций
        И запрос пользователя равен "<user_query>"
        И запрос задаётся откуда-то из региона "Москва"
        И пользователь запрашивает gta с ключом "geoid"
        Когда пользователь запускает поиск
        Тогда он получает непустой ответ
        И все объекты в ответе являются коллекциями
        И у всех коллекций gta с ключом "geoid" равно "<region_id>"

        Примеры: запросы пользователя
            | user_query           | region_id |
            | кафе                 | 213       |
            | музей в москве       | 213       |
            | кафе в питере        | 2         |

    Структура сценария: поиск коллекций по некорректным рубрикам
        Пусть для поиска доступна только вертикаль коллекций
        И запрос пользователя равен "<user_query>"
        И запрос задаётся откуда-то из региона "Москва"
        Когда пользователь запускает поиск
        Тогда он получает пустой ответ

        Примеры: запросы пользователя
            | user_query            |
            | кладбище              |
            | крематорий            |
            | мусорная площадка     |

