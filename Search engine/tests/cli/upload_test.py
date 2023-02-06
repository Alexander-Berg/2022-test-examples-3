from cli_test_utils import check_cli_line


def test_monitoring_command():
    line = "tool upload local " \
           "--regional WORLD " \
           "--evaluation WEB " \
           "--cron 101927 " \
           "--host man " \
           "--name man " \
           "--url https://metrics-calculation.qloud.yandex-team.ru/api/json " \
           "--ui-url https://metrics.yandex-team.ru/mc/compare " \
           "--date 2020-03-31T13:30:00.000+03:00 " \
           "--ytpath //home/robot-search-monitoring/cron_101927/rawSerps-68ad7439-888a-4f70-b3c6-6fab5421bec1 " \
           "--ythost arnold.yt.yandex.net " \
           "--expiration-date 2020-04-30T13:38:28.861+03:00 " \
           "--experiment 01713024230a7b11861d2c9f5d98734c " \
           "--basket 296146"
    args = check_cli_line(line)
    assert args.regional == "WORLD"
    assert args.evaluation == "WEB"
    assert args.cron == 101927
    assert args.host == "man"
    assert args.name == "man"
    assert args.url == "https://metrics-calculation.qloud.yandex-team.ru/api/json"
    assert args.ui_url == "https://metrics.yandex-team.ru/mc/compare"
    assert args.date == "2020-03-31T13:30:00.000+03:00"
    assert args.ytpath == "//home/robot-search-monitoring/cron_101927/rawSerps-68ad7439-888a-4f70-b3c6-6fab5421bec1"
    assert args.ythost == "arnold.yt.yandex.net"
    assert args.expiration_date == "2020-04-30T13:38:28.861+03:00"
    assert args.experiment == "01713024230a7b11861d2c9f5d98734c"
    assert args.basket == "296146"
