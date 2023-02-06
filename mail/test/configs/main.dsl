import list_childs, is_corp from "main.lua";

# создаём базовый клиент для ЧЯ, который умеет по уиду (в т.ч. корповому)
# получать информацию о пользователе
create blackbox_userinfo extractor uid_resolver(long uid) -> userinfo {
    # из ЧЯ получаем информацию о семье пользователя
    get-family-info = true

    [blackbox]
    host = $(BLACKBOX_HOST)
    connections = 10
    pass-referer = false
    timeout = 200ms
    tvm-client-id = $(BLACKBOX_TVM_CLIENT_ID)

    [blackbox.stat]
    prefix = blackbox
    metrics = httpcodes, requesttimes, requesthist
    histogram-ranges = 0, 1, 10, 20, 50, 100, 150, 200, 300, 500, 750, 1000
    precise-histogram = false
    processing-time-stats = false

    [blackbox.http-error-retries]
    interval = 50ms
    count = 1

    [blackbox.io-error-retries]
    interval = 50ms
    count = 1

    [corp-blackbox]
    host = $(CORP_BLACKBOX_HOST)
    connections = 10
    pass-referer = false
    timeout = 200ms
    tvm-client-id = $(CORP_BLACKBOX_TVM_CLIENT_ID)

    [corp-blackbox.stat]
    prefix = corp-blackbox
    metrics = httpcodes, requesttimes, requesthist
    histogram-ranges = 0, 1, 10, 20, 50, 100, 150, 200, 300, 500, 750, 1000
    precise-histogram = false
    processing-time-stats = false

    [corp-blackbox.http-error-retries]
    interval = 50ms
    count = 1

    [corp-blackbox.io-error-retries]
    interval = 50ms
    count = 1
}

# создаём экстрактор, который по family_id получает список членов семьи
create blackbox_family_info extractor family_resolver(string family_id) -> json_object {
    host = $(BLACKBOX_HOST)
    connections = 10
    pass-referer = false
    timeout = 200ms
    tvm-client-id = $(BLACKBOX_TVM_CLIENT_ID)

    [stat]
    prefix = blackbox-family-info
    metrics = httpcodes, requesttimes, requesthist
    histogram-ranges = 0, 1, 10, 20, 50, 100, 150, 200, 300, 500, 750, 1000
    precise-histogram = false
    processing-time-stats = false

    [http-error-retries]
    interval = 50ms
    count = 1

    [io-error-retries]
    interval = 50ms
    count = 1
}

# вложенный экстрактор, который получает список членов семьи, только если
# выставлен флаг resolve_members
create chain extractor get_family_info(string family_id, boolean resolve_members) -> json_object {
    trace critical family_resolver(family_id) -> family_info;

    # если условие в if не выполняется, то будет возвращён null
    # и вычисление family_info не потребуется, так что и вызова экстрактора не
    # произойдёт
    return family_info if is_true(resolve_members);
}

create chain extractor corp_flag(long uid) -> string {
    return "corp" if is_corp(uid.__json_object__) else "non corp";
}

create chain extractor main(long uid, boolean resolve_members) -> json_map {
    trace critical uid_resolver(uid) -> userinfo;

    critical get_family_info(userinfo.__json_object__.family_info.family_id.__string__, resolve_members) -> family_info;

    corp_flag(uid) -> corp_flag;

    list_childs(family_info) -> childs_list;

    create compose_doc extractor fill_doc(
        json_map base,
        json_object userinfo,
        json_object family_info,
        json_object is_corp,
        json_object family_childs_list)
        -> json_map
    {
    }

    create_json_map() -> empty_map;
    fill_doc(
        empty_map,
        userinfo.__json_object__,
        family_info,
        corp_flag.__json_object__,
        childs_list.__json_object__) -> result;
    return result;
}

