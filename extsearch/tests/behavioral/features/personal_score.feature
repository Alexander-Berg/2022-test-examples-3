# language: ru
Функция: получение персонального скора

    Предыстория:
        Пусть выбрана локаль ru_RU
        И запрос задаётся откуда-то из региона "Москва"
        И переданы параметры
            rearr=scheme_Local/Geo/PersonalScore/Enabled=1
            middle_metaopts=GeoFreshProfileServer:TimeOut=1000ms
            middle_metaopts=GeoPersonalScore:TimeOut=1000ms
        И запрошен сниппет "personal_score/1.x"

    # TODO(sobols): uncomment after removing `scheme_Local/Geo/PersonalScore/Enabled=0` from ITS
    # Сценарий: поиск организаций с персональным скором
    #     Пусть запрос пользователя равен "кафе"
    #     И это запрос с passport_uid
    #     Когда пользователь запускает поиск
    #     Тогда хотя бы один документ из top10 имеет непустой сниппет "personal_score/1.x"

    Сценарий: поиск организаций незалогином
        Пусть запрос пользователя равен "кафе"
        Когда пользователь запускает поиск
        Тогда ни один документ из top10 не содержит сниппет "personal_score/1.x"

    Сценарий: поиск организаций без персскора
        Пусть запрос пользователя равен "кладбище"
        И это запрос с passport_uid
        Когда пользователь запускает поиск
        Тогда ни один документ из top10 не содержит сниппет "personal_score/1.x"
