# language: ru
Функция: получение сниппетов

    Предыстория:
        Пусть выбрана локаль ru_RU

    Сценарий: сниппеты города
        Пусть пользователь поискал "Россия, Москва" в регионе "Москва"
        И запрошены сниппеты
            toponym_afisha_events/1.x
            toponym_afisha_cinema/1.x
            related_places/1.x
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top1 имеет непустой сниппет "AfishaEvents"
        Тогда хотя бы один документ из top1 имеет непустой сниппет "AfishaCinema"
        Тогда хотя бы один документ из top1 имеет непустой сниппет "RelatedPlaces"

    Сценарий: сниппет организации yandex_travel/1.x
        Пусть пользователь поискал "отели" в регионе "Москва"
        И запрошен сниппет "yandex_travel/1.x"
        И передан параметр "middle_metaopts" со значением "YandexTravel:TimeOut=1000ms"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "yandex_travel/1.x"
        И каждый сниппет "yandex_travel/1.x" содержит "WasFound"

    Сценарий: тест middle_snippets_oid
        Пусть пользователь поискал "отели" в регионе "Москва"
        И запрошен сниппет "related_places/1.x"
        И запомнил 3-й ответ
        И запрос задаётся откуда-то из региона "Москва"
        Когда пользователь запускает поиск с middle_snippets_oid из запомненного ответа
        Тогда он получает непустой ответ
        И 3-й объект в ответе имеет непустой сниппет "RelatedPlaces"

    Сценарий: сниппеты организаций afisha_json, yabilet_reference
        Пусть пользователь поискал "кинотеатры" в регионе "Москва"
        И запрошен сниппет "afisha_json/1.x"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "afisha_json/1.x"

    Сценарий: сниппеты организаций afisha_json_geozen
        Пусть пользователь поискал "кинотеатры" в регионе "Москва"
        И запрошен сниппет "afisha_json_geozen/1.x"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "afisha_json_geozen/1.x"

    Сценарий: сниппеты организаций businessrating,photos,experimental,masstransit,panoramas,org_wizard_factors
        Пусть пользователь поискал "кафе" в регионе "Москва"
        И запрошены сниппеты
            businessrating/2.x
            photos/2.x
            experimental/1.x
            masstransit/1.x
            panoramas/1.x
            org_wizard_factors/1.x
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "BusinessRating"
        Тогда хотя бы один документ из top10 имеет непустой сниппет "Photos"
        Тогда хотя бы один документ из top10 имеет непустой сниппет "ExperimentalMetaData"
        Тогда хотя бы один документ из top10 имеет непустой сниппет "Stops"
        Тогда хотя бы один документ из top10 имеет непустой сниппет "Panoramas"
        # uncomment or drop this test after GEOSEARCH-6515
        # Тогда хотя бы один документ из top10 имеет непустой сниппет "org_wizard_factors/1.x"

    Сценарий: сниппеты организаций offers_new_auto,offers_used_auto
        Пусть пользователь поискал "автосалон" в регионе "Москва"
        И запрошен сниппет "offers_new_auto/1.x"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "offersNewAuto"

    Сценарий: сниппеты организаций realty
        Пусть пользователь поискал "новостройки" в регионе "Москва"
        И запрошен сниппет "realty/1.x"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "Realty"

    Сценарий: сниппеты организаций route_distances
        Пусть пользователь поискал "кафе" в регионе "Москва"
        И запрошен сниппет "route_distances/1.x"
        И пользователь находится в центре окна поиска
        И переданы параметры
            middle_metaopts=MapsRouterAuto:TimeOut=1000ms
            rearr=scheme_Local/Geo/Router/UseMapsRouter=1
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "RouteDistances"

    Сценарий: сниппеты организаций route_distances (маршрут > 50 км)
        Пусть пользователь поискал "кафе" в регионе "Москва"
        И запрошен сниппет "route_distances/1.x"
        И пользователь находится в точке "37.75,55.25"
        И переданы параметры
            middle_metaopts=MapsRouterAuto:TimeOut=1000ms
            rearr=scheme_Local/Geo/Router/UseMapsRouter=1
        Когда пользователь запускает поиск
        Тогда ни один документ из top10 не содержит сниппет "RouteDistances"

    Сценарий: сниппеты организаций edadeal
        Пусть пользователь поискал "пятерочка" в регионе "Москва"
        И запрошен сниппет "edadeal/1.x"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "edadeal/1.x"

    Сценарий: сниппеты организаций fuel/1.x
        Пусть пользователь поискал "азс" в регионе "Москва"
        И запрошен сниппет "fuel/1.x"
        Когда пользователь запускает поиск
        Тогда хотя бы один документ из top10 имеет непустой сниппет "FuelInfo"

    Сценарий: сниппеты организаций chat_xml/1.x
        Пусть запрошен сниппет "chat_xml/1.x"
        Когда пользователь запускает поиск по пермалинку "1278260588"
        Тогда хотя бы один документ из top1 имеет непустой сниппет "ChatSnippet"

    Сценарий: баннер заправок
        Пусть пользователь поискал "азс" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "gas_stations"

    Сценарий: баннер яндекс заправок
        Пусть пользователь поискал "(provider:gas_stations)(category_id:184105274 | category_id:184105272)" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "yandex_gas_stations"

    Сценарий: баннер яндекс заправок 2
        Пусть пользователь поискал "{"text":"Яндекс.Заправки","what":[{"attr_name":"category_id","attr_values":["184105274"]},{"attr_name":"provider","attr_values":["gas_stations"]}]}" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "yandex_gas_stations"

    Сценарий: баннер лавки
        Пусть пользователь поискал "рыбный магазин" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "lavka"

    Сценарий: баннер лавки в Минске
        Пусть пользователь поискал "рыбный магазин" в регионе "Минск"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе нет баннера "lavka"

    Сценарий: баннер Дептранса Москвы
        Пусть пользователь поискал "text=provider:gr_moscow_business" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "gr_moscow_business"

    Сценарий: баннер БургерКинга
        Пусть пользователь поискал "ресторан быстрое питание улица митинская" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "burgerking-curbside-pickup"

    Сценарий: баннер БургерКинга 2
        Пусть пользователь поискал "макдоналдс" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе нет баннера "burgerking-curbside-pickup"

    Сценарий: баннер БургерКинга 3
        Пусть пользователь поискал "Кинг Авто" в регионе "Москва"
        И запрошен сниппет "banner/1.x"
        Когда пользователь запускает поиск
        Тогда в ответе есть баннер "burgerking-curbside-pickup"

    Структура сценария: сниппет похожих и рекламы на конкурентах
        Пусть запрос пользователя равен "<user_query>"
        И запрос задаётся откуда-то из региона "Немига"
        И запрошены сниппеты
            related_places/1.x
            related_adverts/1.x
        И включено возвращение сниппета конкурентов
        И включена реклама на конкурентах
        И ограничение на рекламу равно 5
        И передан параметр "rearr" со значением "scheme_Local/Geo/Adverts/InjectionBySameRubric/DisableSerpInjection=1"
        И запрошена рекламная страница "navi"
        Когда пользователь запускает поиск
        Тогда cписок похожих организаций не пуст
        И ровно у одной организации в ответе есть сниппет конкурентов уровня выдачи

        Примеры:
            | user_query           |
            | лидо                 |
            | Noor Bar Electro     |