import yt.wrapper as yt

if __name__ == '__main__':
    yt.config.set_proxy('zeno')

    task_id = ''

    result = [

    ]

    if not task_id or not result:
        print('WRONG TASK_ID OR RESULT')
    else:
        yt.write_table(
            '//home/market/testing/market-adv-money/content-manager/moderation/uw-moderation-request-' + task_id + '-moderated',
            result
        )
