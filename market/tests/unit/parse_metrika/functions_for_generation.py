default_bs_params = \
    "{\"block\":{\"elements\":[{\"content\":{\"entity\":\"product\",\"id\": null},\"position\":1}]},\"reqId\":\"19b67040c48341eb90377d8eff782bce\"}"


default_desktop_batch = '19b67040c48341eb90377d8eff782bce'
default_mobile_batch = '1152921504655660164:UR0hJwfCScvyUldttF6uycbDAt9j5yXG1527667028222'

bs_scheme_names = ['host', 'goal', 'referrer', 'params', 'counter_id', 'client_ip', 'puid', 'yandexuid', 'hit_id', 'action', 'user_interface']


# input functions (bs)
def make_hit(action, goal, params="", host="", puid="12345"):
    value = [
        host,
        goal,
        '',
        default_bs_params if params is None or params == '' else params,
        "160656",
        "::ffff:91.107.117.120",
        puid,
        "987654321",
        default_desktop_batch,
        action,
        "TOUCH" if host is not None and host.startswith('m.') else "DESKTOP"
    ]
    ret = dict(zip(bs_scheme_names, value))
    ret['params'] = ret['params'].replace('\\', '')
    return ret


def hit_touch(action, goal, params=""):
    return make_hit(action, goal, params, "m.market.yandex.ru")


def hit_desktop(action, goal, params="", puid="12345"):
    return make_hit(action, goal, params, "market.yandex.ru", puid)


def hit_blue_touch(action, goal, params=""):
    return make_hit(action, goal, params, "m.beru.ru")


def hit_blue_desktop(action, goal, params=""):
    return make_hit(action, goal, params, "beru.ru")


# input functions (mobile)
mobile_scheme_names = ['action', 'puid', 'uuid', 'deviceid', 'yandexuid', 'host', 'goal', 'params', 'client_ip', 'user_interface']


def make_mobile_hit(action, goal, params="", platform=""):
    values = [
        action,
        "12345678",
        "8e58bbadb02725b4a7fc03bbf1ed8411",
        "1f1265219ceae51ba16f14d25da81a00",
        None,
        platform.lower() + '.beru.ru',
        goal,
        params.replace('\\', '') if params is not None and params != '' else None,
        '::ffff:91.107.117.120',
        'MOBILE_APP'
    ]
    return dict(zip(mobile_scheme_names, values))


def hit_ios(action, goal, params=""):
    return make_mobile_hit(action, goal, params, "iOS")


def hit_android(action, goal, params=""):
    return make_mobile_hit(action, goal, params, "android")


# dict of type + id
type_id = {
    'navnode': "6df2c92a2dcb9be4957aa687ab3543f8",
    'model': "1731461904",
    'sku': "10547902",
    'vendor': "1006810",
    'offer': "-AJFsQkuUl8I8UTPQyHpZg",
    'review': "72273985",
    'entrypoint': "51632",
    'hub': "home",
    "article": "74001",
    "retail_shop": "1985533",
    '': "",
}

type_id_unhashed = {
    'navnode': 'showcaseItem8228',
}


# output functions (events)
def make_event(action, place, type, batch, user_interface, puid="12345"):
    """
    I just copy functions from events_metrika_actions.0.json.j2
    Optional types are just arrays of one element (or empty arrays [] for null type)
    """
    id = type_id[type]
    user_dict = {"yandexuid": "987654321", "puid": puid, "uuid": None}
    place_dict = {"place": place, "batch": batch,
                  "client_ip": "::ffff:91.107.117.120", "user_interface": user_interface}
    return [
        user_dict,
        action,
        type,
        id,
        place_dict,
    ]


def event_desktop(action, place, type="model", batch=default_desktop_batch, puid="12345"):
    return make_event(action, place, type, batch, "DESKTOP", puid)


def event_touch(action, place, type="model", batch=default_desktop_batch):
    return make_event(action, place, type, batch, "TOUCH")


def event_app(action, place, type="sku", batch=default_mobile_batch):
    id = type_id[type]
    user_dict = {"yandexuid": None, "puid": "12345678", "uuid": "8e58bbadb02725b4a7fc03bbf1ed8411"}
    place_dict = {"place":  place, "batch": batch,
                  "client_ip": "::ffff:91.107.117.120", "user_interface": "MOBILE_APP"}
    return [
        user_dict,
        action,
        type,
        id,
        place_dict
    ]


hit_functions = {
    "hit_desktop": hit_desktop,
    "hit_blue_desktop": hit_blue_desktop,
    "hit_touch": hit_touch,
    "hit_blue_touch": hit_blue_touch,
    "hit_android": hit_android,
    "hit_ios": hit_ios,
    "event_desktop": event_desktop,
    "event_touch": event_touch,
    "event_app": event_app
}

hit_to_event_mapping = {
    "hit_desktop": event_desktop,
    "hit_blue_desktop": event_desktop,
    "hit_touch": event_touch,
    "hit_blue_touch": event_touch,
    "hit_android": event_app,
    "hit_ios": event_app
}

mobile_functions = {'hit_android', 'hit_ios'}
