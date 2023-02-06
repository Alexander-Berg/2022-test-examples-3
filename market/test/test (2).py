import market.bootcamp.deep_dive_2021.trmigor.converter.proto.data_pb2 as data

import yatest
import yt.wrapper

import os


def check(had, parsed):
    for row in had:
        for parsed_row in parsed:
            is_equal = True

            for key in row.keys():
                if row[key] != parsed_row[key]:
                    is_equal = False
                    break

            if is_equal:
                return True
    return False


def test():
    proxy = os.environ["YT_PROXY"]
    yt.wrapper.config["proxy"]["url"] = proxy
    yt.wrapper.config["proxy"]["enable_proxy_discovery"] = False

    path = "//tmp"
    top_N_path = path + "/top_N"
    bottom_N_path = path + "/bottom_N"
    place_info_path = path + "/place_info"

    schema_top_bottom = [
        {"name": "place", "type": "string"},
        {"name": "request", "type": "string"},
        {"name": "wait_time", "type": "string"},
    ]
    yt.wrapper.create("table", top_N_path, attributes={"schema": schema_top_bottom})
    yt.wrapper.create("table", bottom_N_path, attributes={"schema": schema_top_bottom})

    schema_place = [
        {"name": "place", "type": "string"},
        {"name": "max_wait_time", "type": "string"},
        {"name": "min_wait_time", "type": "string"},
        {"name": "perc_50_00", "type": "string"},
        {"name": "perc_70_00", "type": "string"},
        {"name": "perc_90_00", "type": "string"},
        {"name": "perc_95_00", "type": "string"},
        {"name": "perc_99_00", "type": "string"},
        {"name": "perc_99_50", "type": "string"},
        {"name": "perc_99_99", "type": "string"},
    ]
    yt.wrapper.create("table", place_info_path, attributes={"schema": schema_place})

    rows_top = [{
        "place": "Lorem ipsum",
        "request": "req_" + str(i + 1),
        "wait_time": str(100 - i)
    } for i in range(50)]
    yt.wrapper.write_table(top_N_path, rows_top)

    rows_bottom = [{
        "place": "Lorem ipsum",
        "request": str(i + 1),
        "wait_time": str(i + 51)
    } for i in range(50)]
    yt.wrapper.write_table(bottom_N_path, rows_bottom)

    rows_place = [{
        "place": str(i + 1),
        "max_wait_time": str(i + 101),
        "min_wait_time": str(i + 1),
        "perc_50_00": str(i + 1),
        "perc_70_00": str(i + 2),
        "perc_90_00": str(i + 3),
        "perc_95_00": str(i + 4),
        "perc_99_00": str(i + 5),
        "perc_99_50": str(i + 6),
        "perc_99_99": str(i + 7)
    } for i in range(10)]
    yt.wrapper.write_table(place_info_path, rows_place)

    converter = yatest.common.binary_path("market/bootcamp/deep_dive_2021/trmigor/converter/converter")
    args = [
        "--cluster", proxy,
        "--token", "",
        "--table-path", path,
        "--result-path", "."
    ]
    yatest.common.execute([converter] + args)

    parsed = []
    with open("./top_N.dat", "rb") as top:
        requests = data.TRequests()
        requests.ParseFromString(top.read())
        for field in requests.Fields:
            parsed.append({
                "place": field.Place,
                "request": field.Request,
                "wait_time": field.WaitTime
            })
    assert(check(rows_top, parsed))

    parsed = []
    with open("./bottom_N.dat", "rb") as bottom:
        requests = data.TRequests()
        requests.ParseFromString(bottom.read())
        for field in requests.Fields:
            parsed.append({
                "place": field.Place,
                "request": field.Request,
                "wait_time": field.WaitTime
            })
    assert(check(rows_bottom, parsed))

    parsed = []
    with open("./place_info.dat", "rb") as place_info:
        places = data.TPlaces()
        places.ParseFromString(place_info.read())
        for field in places.Fields:
            parsed.append({
                "place": field.Place,
                "max_wait_time": field.MaxWaitTime,
                "min_wait_time": field.MinWaitTime,
                "perc_50_00": field.Perc5000,
                "perc_70_00": field.Perc7000,
                "perc_90_00": field.Perc9000,
                "perc_95_00": field.Perc9500,
                "perc_99_00": field.Perc9900,
                "perc_99_50": field.Perc9950,
                "perc_99_99": field.Perc9999
            })
    assert(check(rows_place, parsed))
