import yt.clickhouse as chyt
import yt.wrapper as yt
from datetime import datetime as dt


def execute_query(query):
    yt_client = yt.YtClient("hahn")
    start_time = dt.now()
    print(f"Executing query:\n{query}")
    list(chyt.execute(query, alias="*apershukov_test", client=yt_client))
    print(f"Executed in {dt.now() - start_time}")
