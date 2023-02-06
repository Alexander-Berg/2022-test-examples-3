from dataclasses import dataclass
from typing import List

from travel.avia.country_restrictions.lib.types import InformationTable


n = None


@dataclass
class GeoPoint:
    point_key: str
    geo_id: int


def matrix_to_rows(countries: List[GeoPoint], metric_types, matrix):
    result = []
    for country, matrix_row in zip(countries, matrix):
        row = {m.name: m.generate_metric(v) for m, v in zip(metric_types, matrix_row)}
        row['point_key'] = country.point_key
        row['key'] = country.geo_id
        result.append(row)

    return result


def matrix_to_information_table(countries: List[GeoPoint], metric_types, matrix) -> InformationTable:
    rows = matrix_to_rows(countries, metric_types, matrix)
    result = {}
    for row in rows:
        point_key = row.pop('point_key')
        row.pop('key')
        result[point_key] = row
    return result


def information_table_to_matrix(data, countries: List[GeoPoint], metric_types):
    data_matrix = []

    for country in countries:
        country_data = data.get(country.point_key, {})
        row = []
        for metric_type in metric_types:
            metric = country_data.get(metric_type.name, None)
            if metric is None:
                row.append(None)
            else:
                row.append(metric.value)
        data_matrix.append(row)

    return data_matrix
