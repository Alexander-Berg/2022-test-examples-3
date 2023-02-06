from zlib import compress
from fan.lists.csv_maillist import parse_csv_data, get_preview, get_subscribers_number
from fan.models import Maillist
from fan.testutils.maillist_fixtures import maillist_csv_content


def store_maillist(account, title, filename):
    csv_maillist = parse_csv_data(maillist_csv_content())
    maillist = Maillist.objects.create(
        account=account,
        title=title,
        filename=filename,
        data=compress(maillist_csv_content().encode("utf-8")),
        preview=get_preview(csv_maillist),
        size=get_subscribers_number(csv_maillist),
    )
    return maillist


def store_n_maillists(account, n):
    for i in range(n):
        store_maillist(account, "Список рассылки {}".format(i), "maillist{}.csv".format(i))
