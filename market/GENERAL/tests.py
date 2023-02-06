import abc
from typing import List, Dict, Any, Optional

import yt.wrapper as yt
import yt.clickhouse as chyt


def columns_to_clickhouse(
    columns: List[str],
    wrap_function: str = None,
    aliases: List[str] = None
) -> str:
    """
    Заворачивает список названий колонок в строку, подходящую для вставки в Clickhouse
    :param columns: список названий колонок
    :param wrap_function: название функции, которую нужно применить к каждой колонке
    :param aliases: список алиасов, по длине должен совпадать со списком колонок
    :return: строка для вставки в запрос
    """
    if aliases is not None and len(aliases) != len(columns):
        raise ValueError('Length of aliases must be equal to the length of columns')

    mask = '"{col}"'
    if wrap_function is not None:
        mask = f'{wrap_function}({mask})'
    if aliases is not None:
        mask = f'{mask} as {{alias}}'

    wrapped = map(
        lambda zipped: mask.format(col=zipped[0], alias=zipped[1]),
        zip(columns, aliases or [''] * len(columns))
    )
    return ', '.join(wrapped)


class FeatureSourceError(Exception):
    def __init__(self, *args):
        super().__init__(*args)


class FeatureSourceTest(metaclass=abc.ABCMeta):
    def __init__(self, yt_client: yt.YtClient):
        self.yt_client = yt_client
        self.status = None
        self.error = None

    def execute_chyt_query(self, query: str) -> List[Dict[str, Any]]:
        return list(
            chyt.execute(
                query,
                alias='*ch_public',
                client=self.yt_client
            )
        )

    def run(self, *args, **kwargs) -> None:
        self._run(*args, **kwargs)
        if not self.status:
            raise FeatureSourceError(self.error)

    @abc.abstractmethod
    def _run(self, *args, **kwargs) -> None:
        pass


class UniqueKeysTest(FeatureSourceTest):
    """
    Тестирует, что все переданные ключи в таблице уникальны
    """
    def _run(self, table: str, keys: List[str]) -> None:
        query = """
            select {keys}
            from `{table}`
            group by {keys}
            having count(*) > 1
            limit 100;
        """.format(
            table=table,
            keys=columns_to_clickhouse(keys)
        )

        result = self.execute_chyt_query(query)
        self.status = len(result) == 0
        if not self.status:
            self.error = f'Table {table} violates unique key constraint for keys: {result}'


class FeatureInformativeTest(FeatureSourceTest):
    """
    Тестирует, что у всех фичей в таблице более одного значения (исключая null)
    """
    def _run(self, table: str, keys: List[str], ignore_columns: Optional[List[str]]) -> None:
        schema = self.yt_client.get_attribute(table, 'schema')
        ignore_columns = ignore_columns or []
        feature_columns = [
            col['name'] for col in schema if (col['name'] not in keys) and (col['name'] not in ignore_columns)
        ]

        query = """
            select {feature_values}
            from `{table}`;
        """.format(
            table=table,
            feature_values=columns_to_clickhouse(
                feature_columns,
                wrap_function='uniq',
                aliases=feature_columns
            )
        )

        result = self.execute_chyt_query(query)
        non_informative_features = list(
            filter(
                lambda item: item[1] <= 1,
                result[0].items()
            )
        )
        self.status = len(non_informative_features) == 0
        if not self.status:
            self.error = f'The following features have only one value: {non_informative_features}'
