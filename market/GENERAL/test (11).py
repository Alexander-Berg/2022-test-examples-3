import re
from datetime import datetime
from typing import List

import qb2.api.v1.extractors as se
from nile.api.v1 import datetime as dt
from yt.wrapper import YtClient

from market.monetize.stapler.v2.args import Arg
from market.monetize.stapler.v2.nile import NileTaskAbc


class ComputeVersionSassNileTask(NileTaskAbc):
    def get_task_args(self) -> List[Arg]:
        pass

    def run(self) -> str:

        # [Глобальные переменные]
        START_DATE = '2021-01-01'
        if START_DATE is None:
            START_DATE = datetime.today().strftime('%Y-%m-%d')

        END_DATE = None
        if END_DATE is None:
            END_DATE = datetime.today().strftime('%Y-%m-%d')

        # = [Необходимые пути] ===========================================
        PATH_OUTPUT = '//home/market-analyst/baytekov/local_sales/{}'
        PATH_SASS = '//home/market/production/replenishment/manual/stable_assortments/{}'

        # = [Вспомогательные функции] ====================================
        def get_date_map():
            # Получаем ТОЛЬКО ДАТЫ из директории
            p = re.compile(r"^\d{4}-\d{2}-\d{2}$")
            date_list = [s for s in YtClient('hahn').list(PATH_SASS[:-3]) if p.match(s)] + [END_DATE]

            # Составляем списки дат соответствия
            return dict([(from_date, list(dt.date_range(from_date, to_date))[:-1]) for from_date, to_date in zip(date_list[:-1], date_list[1:])])

        def map_all(dates4tbl):
            def map_all(records):
                for record in records:
                    for date in dates4tbl:
                        yield record.transform(creation_date=date)

            return map_all

        # = [ Финальный расчет] ==========================================
        date_map = get_date_map()

        job = self.job

        job.concat(*[
            job.table(
                PATH_SASS.format(tbl_path)
            ).qb2(
                log='generic-yson-log',
                fields=[
                    se.log_field('warehouse_id').rename('local_wh_id'),
                    se.log_field('msku'),
                    se.const('is_gm', 1)
                ]
            ).map(map_all(tbl_dates))
            for tbl_path, tbl_dates in date_map.items()
        ]).put(
            PATH_OUTPUT.format('intermediate/sass_versioned')
        )

        job.run()
