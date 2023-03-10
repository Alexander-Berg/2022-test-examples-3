# language: ru
Функция: подмешивание рекламы в геопоиске

    Предыстория:
        Пусть запрошена рекламная страница "maps"
        И пользователь запрашивает gta с ключом "boosted_advert"

    Сценарий: подмешивание рекламы в топ списка
        Пусть запрос пользователя равен "где поесть"
        И запрос задаётся откуда-то из региона "Москва"
        И ограничение на рекламу равно 10
        Когда пользователь запускает поиск
        Тогда первые 10 организаций - геопродукты

    Сценарий: поиск похожих организаций по запросу
        Пусть запрос пользователя равен "kfc"
        И запрос задаётся откуда-то из региона "Немига"
        И запрошен сниппет "experimental/1.x"
        И включена реклама на конкурентах
        И ограничение на рекламу равно 5
        Когда пользователь запускает поиск
        Тогда cписок конкурентов не пуст

    Сценарий: запрос сниппета конкурентов для выдачи
        Пусть запрос пользователя равен "kfc"
        И запрос задаётся откуда-то из региона "Немига"
        И запрошен сниппет "related_adverts/1.x"
        И включено возвращение сниппета конкурентов
        И передан параметр "rearr" со значением "scheme_Local/Geo/Adverts/InjectionBySameRubric/DisableSerpInjection=1"
        И ограничение на рекламу равно <maxadv>
        Когда пользователь запускает поиск
        Тогда ровно у одной организации в ответе есть сниппет конкурентов уровня выдачи

        Примеры:
            | maxadv |
            | 5      |
            | 0      |

    Сценарий: запрос сниппета конкурентов для каждой организации
        Пусть запрос пользователя равен "лидо"
        И запрос задаётся откуда-то из региона "Минск"
        И запрошен сниппет "related_adverts_1org/1.x"
        И ограничение на рекламу равно 5
        Когда пользователь запускает поиск
        Тогда у всех организаций в ответе есть сниппет конкурентов уровня организации
