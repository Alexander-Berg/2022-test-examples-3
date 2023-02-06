class MultiTablesMockYtClient:
    def __init__(self, tables):
        self.tables = tables

    def read_table(self, table):
        table_name: str = table._path_object
        return self.tables[table_name]


class SingleTableMockYtClient:
    def __init__(self, table, result_data):
        self.table = table
        self.result_data = result_data

    def read_table(self, table):
        return table

    def write_table(self, table_path, rows):
        assert rows == self.result_data
