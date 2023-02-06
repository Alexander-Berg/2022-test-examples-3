import datetime

import pytz

from travel.avia.stat_admin.data.models import PartnerReview


def create(point_from, point_to,
           date_forward=None, hit_time=None,
           review_time=None, price=1000,
           price_diff_abs=10, price_diff_rel=0.01, wizard_redir_key=''):
    date_forward = date_forward or datetime.date(2017, 9, 2)
    hit_time = hit_time or datetime.datetime(2017, 9, 1, tzinfo=pytz.utc)
    review_time = review_time or datetime.datetime(2017, 9, 1, tzinfo=pytz.utc)

    result = PartnerReview(
        point_from=point_from,
        point_to=point_to,
        date_forward=date_forward,
        hit_time=hit_time,
        price=price,
        review_time=review_time,
        price_diff_abs=price_diff_abs,
        price_diff_rel=price_diff_rel,
        wizard_redir_key=wizard_redir_key
    )
    result.save()
    return result
