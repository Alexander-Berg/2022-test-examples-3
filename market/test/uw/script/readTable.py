import yt.wrapper as yt

if __name__ == '__main__':
    yt.config.set_proxy('zeno')

    task_id = ''

    if not task_id:
        print('WRONG TASK_ID')
    else:
        for row in yt.read_table(
            '//home/market/testing/market-adv-money/content-manager/moderation/uw-moderation-request-' + task_id
        ):
            print(row)
