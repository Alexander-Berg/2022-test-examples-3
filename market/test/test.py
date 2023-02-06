import market.bootcamp.deep_dive_2021.babywizard.converter.proto.analytics_pb2 as analytics_pb2

import yt.wrapper as yt
import yatest

import os


def check_all_found(rows, parsed_rows):
    for row in rows:
        found = False
        for parsed_row in parsed_rows:
            is_eq = True

            for key in row.keys():
                if row[key] != parsed_row[key]:
                    is_eq = False
                    break

            if is_eq:
                found = True
                break
        if not found:
            return False
    return True


def test():
    yt_proxy = os.environ["YT_PROXY"]

    yt.config["proxy"]["url"] = yt_proxy
    yt.config["proxy"]["enable_proxy_discovery"] = False

    table = "//tmp/table"

    schema = [
        {"name" : "id", "type" : "int64"},
        {"name" : "name", "type" : "string"},
        {"name" : "avg_click_price", "type" : "double"},
        {"name" : "min_click_price", "type" : "uint64"},
        {"name" : "max_click_price", "type" : "uint64"},
    ]
    yt.create_table(table, attributes={"schema": schema})

    rows = [
        {"id" : 3, "name" : "Samovar", "min_click_price" : 1, "max_click_price" : 10, "avg_click_price" : 7.5},
        {"id" : 1, "name" : "Ogurec", "min_click_price" : 2, "max_click_price" : 8, "avg_click_price" : 7.0},
        {"id" : 4, "name" : "Pomidor", "min_click_price" : 1, "max_click_price" : 23, "avg_click_price" : 17.7},
        {"id" : 2, "name" : "Русский помидор", "min_click_price" : 100, "max_click_price" : 1000, "avg_click_price" : 300.3},
    ]

    yt.write_table(table, rows)

    converter_program = yatest.common.binary_path("market/bootcamp/deep_dive_2021/babywizard/converter/converter")
    cmd_args = [
        "--cluster", yt_proxy,
        "--token", "just-required",
        "--src", table,
        "--dst", "./categories.dat"
    ]
    yatest.common.execute([converter_program] + cmd_args)

    parsed_rows = []
    with open("./categories.dat", "rb") as f:
        analytics = analytics_pb2.TAnalytics()
        analytics.ParseFromString(f.read())
        for note in analytics.Notes:
            parsed_rows.append({
                "id": note.Id,
                "name": note.Name,
                "avg_click_price": note.AvgClickPrice,
                "min_click_price": note.MinClickPrice,
                "max_click_price": note.MaxClickPrice
            })

    assert check_all_found(rows, parsed_rows)
