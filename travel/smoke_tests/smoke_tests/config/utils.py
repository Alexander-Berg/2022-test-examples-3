from datetime import datetime, timedelta

import pytz

msk_tz = pytz.timezone('Europe/Moscow')
msk_now = pytz.UTC.localize(datetime.utcnow()).astimezone(msk_tz)
msk_now_str = '{:%Y-%m-%d %H:%M}'.format(msk_now)
msk_today_str = '{:%Y-%m-%d}'.format(msk_now)
msk_tomorrow_str = '{:%Y-%m-%d}'.format(msk_now + timedelta(days=1))
msk_yesterday_str = '{:%Y-%m-%d}'.format(msk_now - timedelta(days=1))

context = {
    'msk_now': msk_now_str,
    'msk_today': msk_today_str,
    'msk_tomorrow': msk_tomorrow_str,
    'msk_yesterday': msk_yesterday_str,
}
