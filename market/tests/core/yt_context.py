from testenv import testenv
from tables import Directory, ConfigTable, ErpInputTable, DemandInputTable, HistoryTable, EstHistoryTable, MarginTable
from yt.wrapper import YtClient, YPath
import utils
import os, logging


class YtContext:
    """
    Manages tables needed for dynamic pricing:
    1. All tables are created on context enter
    2. All tables are removed on context exit
    """

    def __enter__(self):
        logging.info("Create all necessary tables...")

        self.client.create("map_node", self.root, recursive=True)
        for name, item in self.__items.items():
            item.create(self.client)

        logging.info("Create all necessary tables... OK; root YT-node: " + self.root)
        return self

    def __exit__(self, *args, **kwargs):
        logging.info("Remove all created items...")
        self.client.remove(self.root, recursive=True, force=True)
        logging.info("Remove all created items... OK")

    def __init__(self, env):
        self.__module_root = "//" + env.MODULE_NAME
        self.__client = YtClient(proxy=os.environ["YT_PROXY"])
        self.__items = {}

    def __get_or_create(self, name, item_class):
        if name not in self.__items:
            self.__items[name] = item_class(self.root + "/" + name)
        return self.__items[name]

    @property
    def cluster(self):
        return self.client.config["proxy"]["url"]

    @property
    def root(self):
        return self.__module_root

    @property
    def client(self):
        return self.__client

    @property
    def config(self):
        return self.__get_or_create("config", ConfigTable)

    @property
    def erp_input(self):
        return self.__get_or_create("erp_input", ErpInputTable)

    @property
    def demand_input(self):
        return self.__get_or_create("demand_input", DemandInputTable)

    @property
    def group_table(self):
        return self.__get_or_create("input_all", ErpInputTable)  # has the same schema

    @property
    def history_table(self):
        return self.__get_or_create("history", HistoryTable)

    @property
    def est_history_table(self):
        return self.__get_or_create("est_history", EstHistoryTable)

    @property
    def margins_table(self):
        return self.__get_or_create("margin", MarginTable)

    def read_whole(self, path):
        return [row for row in self.client.read_table(self.root + "/" + path)]




