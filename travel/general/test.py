# -*- coding: utf-8 -*-

import os
# from copy import copy
# from itertools import combinations
from functools import partial

from nile.api.v1 import (
    extractors as ne,
    aggregators as na,
    filters as nf,
    statface as ns,
    Record,
    cli
)

from nile.files import LocalFile

from qb2.api.v1 import (
    extractors as se,
    filters as sf,
    resources as sr
)


directory = os.environ['JWD']
os.sys.path.append(directory)
from common import BAD_TEST_IDS, DEFAULT_DIR, TRAVEL_RUBRICS, add_totals, norm_rubric, WizardType, is_one_org

@cli.statinfra_job
def make_job(job, options, nirvana, statface_client):
    """Standart function according to Statistics conventions,
    see https://clubs.at.yandex-team.ru/statistics/1143"""

    dates = options.dates
    if len(dates) > 1:
        suffix = "{first}_{last}".format(first=dates[0], last=dates[-1])
    else:
        suffix = dates[0]

    job_root = nirvana.directories[0] if nirvana.directories else DEFAULT_DIR

    job = job.env(
	files=[LocalFile(os.path.join(directory, 'common.py'))],
        templates=dict(job_root=job_root,
                       suffix=suffix,
                       )
                 )

    input_table = nirvana.input_tables[0] if nirvana.input_tables else \
       '$job_root/user_sessions/@dates'
    output_table = nirvana.output_tables[0] if nirvana.output_tables else \
        '$job_root/test/$suffix'

    job.table(input_table)\
        .project("region",
                 "device",
                 "search_props",
                 num_of_hotel_urls=ne.custom(lambda x: str(len(x)), "hotel_urls"),
		 mn_org_type=ne.custom(get_type, "blocks"),
                 request_after_carousel_click=ne.custom(lambda x: "after" if x else "before", "carousel_clicked_org"),
                 entity_search=ne.custom(lambda x: "yes" if x.get(
                     WizardType.EntitySearch) is not None else "no", "blocks"),
                 travel_1org=ne.custom(is_one_org, "blocks"),
                 carousel=ne.custom(lambda blocks, test_ids: blocks.get(
                     WizardType.Carousel) is not None and not BAD_TEST_IDS.intersection(test_ids), "blocks", "test_ids"),
                 travel_1org_carousel=ne.custom(lambda x: (x.get(WizardType.Carousel) is not None) and (
                     x.get(WizardType.TravelOneOrg) is not None), "blocks"),
                 vanilla_1org_carousel=ne.custom(lambda x: (x.get(WizardType.Carousel) is not None) and (
                     x.get(WizardType.OneOrg) is not None), "blocks"),
                 empty_clicked_carousel=ne.custom(lambda blocks, carousel_clicked_org: (blocks.get(WizardType.Carousel) is not None) and (blocks.get(
                     WizardType.OneOrg) is None) and (blocks.get(WizardType.TravelOneOrg) is None) and carousel_clicked_org > 0, "blocks", "carousel_clicked_org"),
                 fielddate=ne.custom(lambda x: x.split("T")[0], "time_isoformatted"))\
       .put(output_table)\

    return job


if __name__ == "__main__":
    cli.run()
