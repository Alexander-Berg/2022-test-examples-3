from experiment import run_experiment, Method, Case
from utils import execute_query
import yt.wrapper as yt

TMP_PATH = "//home/market/development/crm/apershukov/chyt/tmp"


def clean_up():
    yt.remove(TMP_PATH, force=True, recursive=True)
    yt.create("map_node", TMP_PATH)


def sort_with_chyt(input_path):
    query = f"""CREATE TABLE `{TMP_PATH + "/output"}` engine YtTable() order by (id_value, id_type) as
        SELECT
            id_value,
            id_type
        FROM `{input_path}`
        ORDER BY id_value, id_type"""

    execute_query(query)


def sort_with_yt(input_path):
    output_path = TMP_PATH + "/output"

    yt.create("table", path=output_path, attributes={
        "schema": [
            {
                "name": "id_value",
                "type": "string",
                "sort_order": "ascending"
            },
            {
                "name": "id_type",
                "type": "string",
                "sort_order": "ascending"
            }
        ]
    })

    yt.run_sort(
        source_table=input_path,
        destination_table=output_path,
        sort_by=["id_value", "id_type"]
    )


def main():
    run_experiment(
        clean_up_action=clean_up,
        methods=[
            Method(description="CHYT", action=sort_with_chyt),
            Method(description="YT", action=sort_with_yt)
        ],
        cases=[
            Case("2k", input_path="//home/market/development/crm/apershukov/chyt/sort_data/2k"),
            Case("1M", input_path="//home/market/development/crm/apershukov/chyt/sort_data/1M"),
            Case("16M", input_path="//home/market/development/crm/apershukov/chyt/sort_data/16M"),
            Case("128M", input_path="//home/market/development/crm/apershukov/chyt/sort_data/128M")
        ]
    )


if __name__ == "__main__":
    yt.config["proxy"]["url"] = "hahn"
    main()
