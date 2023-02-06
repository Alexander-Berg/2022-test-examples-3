import yaml
from hamcrest import (assert_that,
                      has_length,
                      has_entry,
                      has_entries,
                      has_properties)

from pymdb.operations import AddMailishEntry, EraseMailishSecurityLocks, InitExistingFolderAsMailish, \
    InvalidateMailishAuth, SaveMailishAccount, UpdateMailishDownloadedRange, UpdateMailishFolder, \
    DeleteMailishEntries, IncrementMailishEntryErrorsCount, MoveMailishMessages, UpdateMailishAccountLastSync, \
    DeleteMailishFolderEntries
from pymdb.types import MailishFolderInfo, MailishCoordinates, MailishMoveCoords
from tests_common.pytest_bdd import given, when, then


@when(u'we add mailish entries')
def step_add_mailish_entries(context):
    for row in context.table:
        context.make_operation(AddMailishEntry)(
            fid=int(row['fid']),
            mailish_coords=MailishCoordinates(imap_id=int(row['imap_id']), imap_time=row['imap_time'])
        ).commit()


@when(u'we try to add mailish entries as "{op_id}"')
def step_try_add_mailish_entries(context, op_id):
    assert_that(context.table.rows, has_length(1))

    row = context.table[0]
    context.operations[op_id] = context.make_async_operation(
        AddMailishEntry
    )(
        fid=int(row['fid']),
        mailish_coords=MailishCoordinates(imap_id=int(row['imap_id']), imap_time=row['imap_time'])
    )


@when(u'we erase mailish security locks')
def step_erase_mailish_security_locks(context):
    context.make_operation(EraseMailishSecurityLocks)().commit()


@when(u'we init existing folder as mailish')
def step_init_existing_folder_as_mailish(context):
    for row in context.table:
        context.make_operation(InitExistingFolderAsMailish)(
            fid=int(row['fid']),
            mailish_folder_info=MailishFolderInfo(path=row['path'], uidvalidity=int(row['uidvalidity']))
        ).commit()


@when(u'we try init existing folder as mailish with op_id "{op_id}"')
def step_try_init_existing_folder_as_mailish(context, op_id):
    assert_that(context.table.rows, has_length(1))

    row = context.table[0]
    context.operations[op_id] = context.make_async_operation(InitExistingFolderAsMailish)(
        fid=int(row['fid']),
        mailish_folder_info=MailishFolderInfo(path=row['path'], uidvalidity=int(row['uidvalidity']))
    )


@when(u'invalidate mailish token {token_id}')
def step_invalidate_mailish_auth(context, token_id):
    context.make_operation(InvalidateMailishAuth)(
        token_id=token_id
    ).commit()


@when(u'we save account "{name:UserName}"')
def step_save_mailish_account(context, name):
    assert context.text
    data = yaml.safe_load(context.text)
    for row in data:
        context.make_operation(SaveMailishAccount, uid=context.users[name])(
            account=row
        ).commit()


@when(u'we updating downloaded range')
def step_update_downloaded_range(context):
    for row in context.table:
        context.make_operation(UpdateMailishDownloadedRange)(
            fid=int(row['fid']),
            range_start=int(row['range_start']),
            range_end=int(row['range_end'])
        ).commit()


@when(u'we try updating downloaded range as "{op_id}"')
def step_try_update_downloaded_range(context, op_id):
    assert_that(context.table.rows, has_length(1))

    row = context.table[0]
    context.operations[op_id] = context.make_async_operation(UpdateMailishDownloadedRange)(
        fid=int(row['fid']),
        range_start=int(row['range_start']),
        range_end=int(row['range_end'])
    )


@when(u'we updating mailish folder')
def step_update_mailish_folder(context):
    for row in context.table:
        context.make_operation(UpdateMailishFolder)(
            fid=int(row['fid']),
            mailish_folder_info=MailishFolderInfo(path=row['path'], uidvalidity=int(row['uidvalidity']))
        ).commit()


@when(u'we delete mailish folder entries')
def step_delete_mailish_folder_entries(context):
    context.make_operation(DeleteMailishFolderEntries)(
        fids=[int(row['fid']) for row in context.table]
    ).commit()


@when(u'we try delete mailish folder entry as "{op_id}"')
def step_try_delete_mailish_folder_entries(context, op_id):
    context.operations[op_id] = context.make_async_operation(
        DeleteMailishFolderEntries
    )(
        fids=[int(row['fid']) for row in context.table]
    )


@when(u'we deleting mailish entry')
def step_delete_mailish_entry(context):
    for row in context.table:
        context.make_operation(DeleteMailishEntries)(
            fid=int(row['fid']),
            imap_ids=[int(row['imap_id']), ]
        ).commit()


@when(u'we try delete mailish entry as "{op_id}"')
def step_try_delete_mailish_entries(context, op_id):
    assert_that(context.table.rows, has_length(1))

    row = context.table[0]
    context.operations[op_id] = context.make_async_operation(
        DeleteMailishEntries
    )(
        fid=int(row['fid']),
        imap_ids=[int(row['imap_id']), ]
    )


@when(u'we increment mailish entry errors count')
def step_increment_mailish_errors_count(context):
    for row in context.table:
        context.make_operation(IncrementMailishEntryErrorsCount)(
            fid=int(row['fid']),
            imap_id=int(row['imap_id']),
            errors=int(row['errors']) if 'errors' in row.headings else 1
        ).commit()


@when(u'we move mailish messages to fid {to_fid} from fid {from_fid}')
def step_move_mailish_messages(context, to_fid, from_fid):
    move_coords = []
    for row in context.table:
        move_coords.append(MailishMoveCoords(src_imap_id=int(row['from_id']), dst_imap_id=int(row['to_id'])))

    context.make_operation(MoveMailishMessages)(
        src_fid=from_fid,
        dst_fid=to_fid,
        mailish_move_coords=move_coords
    ).commit()


@when(u'we try to increment mailish entry errors count as "{op_id}"')
def step_try_increment_mailish_errors(context, op_id):
    assert_that(context.table.rows, has_length(1))

    row = context.table[0]
    context.operations[op_id] = context.make_async_operation(
        IncrementMailishEntryErrorsCount
    )(
        fid=int(row['fid']),
        imap_id=int(row['imap_id']),
        errors=int(row['errors'])
    )


@when(u'we update mailish account last sync with new date "{new_sync_date}"')
def step_update_mailish_account_last_sync(context, new_sync_date):
    context.make_operation(UpdateMailishAccountLastSync)(
        last_sync=new_sync_date
    ).commit()


@then(u'we have {data_count:d} auth data')
def step_count_mailish_auth_data(context, data_count):
    auth_res = context.qs.mailish_auth_count()[0]
    assert_that(auth_res, has_entry('count', data_count))


@then(u'we have {count:d} security locks')
def step_count_security_locks(context, count):
    count_res = context.qs.mailish_security_locks_count()[0]
    assert_that(count_res, has_entry('count', count))


@then(u'we have mailish data for fid {fid}')
def step_compare_mailish_data_for_folder(context, fid):
    folder_data = context.qs.mailish_folder(fid=fid)[0]
    expected_data = yaml.safe_load(context.text)

    assert_that(folder_data, has_entries(expected_data))


@then(u'we have downloaded range for fid {fid} equals ({range_start:d}, {range_end:d})')
def step_compare_downloaded_range_for_folder(context, fid, range_start, range_end):
    folder_res = context.qs.mailish_folder(fid=fid)[0]

    assert_that(folder_res, has_entry('range_start', range_start))
    assert_that(folder_res, has_entry('range_end', range_end))


@then(u'we have {count:d} items in mailish folder {fid}')
def step_count_mailish_entries(context, count, fid):
    count_res = context.qs.mailish_count(fid=fid)[0]
    assert_that(count_res, has_entry('count', count))


@then(u'we have {count:d} mailish folders')
def step_count_mailish_folder_entries(context, count):
    count_res = context.qs.mailish_folders_count()[0]
    assert_that(count_res, has_entry('count', count))


@then(u'we have errors count equals {count:d} for entry')
def step_mailish_errors_count(context, count):
    assert_that(context.table.rows, has_length(1))
    row = context.table[0]

    mailish_data = context.qs.mailish_data_by_imap_coords(fid=row['fid'], imap_id=row['imap_id'])[0]
    assert_that(mailish_data, has_entry('errors', count))


@then(u'token "{token_id}" has data')
def step_check_auth_data(context, token_id):
    expected_data = yaml.safe_load(context.text)

    mailish_auth_data = context.qs.mailish_auth_data_by_token_id(token_id=token_id)[0]
    assert_that(mailish_auth_data, has_entries(expected_data))


@then(u'account has data')
def step_check_account_data(context):
    expected_data = yaml.safe_load(context.text)

    mailish_account = context.qs.mailish_accounts()[0]
    assert_that(mailish_account, has_properties(expected_data))


@given(u'inited mailish user with fid {fid}')
def step_prepare_mailish_user(context, fid):
    context.execute_steps('''
        When we initialize new user "Mailish"
        When we save account "Mailish"
        """
        -   email: test@mail.ru
            imap_login: test@mail.ru
            imap_credentials: qwerty
            imap_server: imap.mail.ru
            imap_port: 993
            imap_ssl: True
            smtp_login: test@mail.ru
            smtp_credentials: qwerty
            smtp_server: smtp.mail.ru
            smtp_port: 465
            smtp_ssl: True
            token_id: '125'
            auth_type: password
            auth_data: qwerty
            uuid: 13a4c6
            oauth_app: mailru-o2
            last_sync: !!timestamp "1996-07-18 02:44:25 -7"
        """
        And we init existing folder as mailish
        | fid | path  | uidvalidity |
        | %s  | INBOX | 109291      |
             ''' % fid)
