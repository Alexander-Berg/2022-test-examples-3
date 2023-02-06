import time
from datetime import timedelta
from hamcrest.core.base_matcher import BaseMatcher
from hamcrest import (
    assert_that,
    equal_to,
    empty,
    is_,
    is_not,
    only_contains,
    is_in,
    any_of,
    has_length,
)

from tests_common.mdb import user_mdb_queries
from behave import then


class RetriesMatcher(BaseMatcher):
    def __init__(self, sleep_times, matcher):
        self.sleep_times = sleep_times
        self.matcher = matcher
        self._last_result = None

    def _matches(self, item):
        if not callable(item):
            raise RuntimeError('expect callable item, got %r', item)
        for try_sleep in self.sleep_times:
            self._last_result = item()
            if self.matcher.matches(self._last_result):
                return True
            time.sleep(try_sleep)
        return False

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('last result of ') \
                            .append_description_of(item) \
                            .append_text(' ')
        self.matcher.describe_mismatch(self._last_result, mismatch_description)

    def describe_to(self, description):
        retries_td = timedelta(seconds=sum(self.sleep_times))
        description.append_description_of(self.matcher) \
                   .append_text(' [') \
                   .append_text('after %d retries ' % len(self.sleep_times)) \
                   .append_text('with total duration %s' % str(retries_td)) \
                   .append_text(']')


SLEEP_TIMES = [5.] * 30


def with_retries(matcher):
    return RetriesMatcher(SLEEP_TIMES, matcher)


@then(u'folder was added to shared_folders table in db with name "{name}"')
def step_folder_added_to_shared(context, name):
    uid = context.uid
    fid = context.shared_folder_fid

    with user_mdb_queries(context, uid) as q:
        shared_fids = q.shared_fids()
        assert fid in shared_fids, 'Expected fid {} to be shared. Shared fids: {}'.format(fid, shared_fids)

        folders = {folder.fid: folder for folder in q.folders()}
        assert fid in folders, 'Expected fid {} to be in folders {}'.format(fid, folders)
        assert folders[fid].name == name, 'Expected folder name to be {}. Folder: {}'.format(name, folders[fid])


@then(u'owner has new subscriber')
@then(u'all owner shared folders have new subscriber')
def step_has_new_subscriber(context):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    fids = context.shared_folders_parents.keys()

    with user_mdb_queries(context, owner_uid) as q:
        subs = q.shared_folder_subscriptions()
        fid_subs = [sub for sub in subs if sub.fid in fids]
        assert len(fid_subs) >= len(fids), \
            'Expected to have subscriptions on fids {}. Subscriptions: {}'.format(fids, subs)

        expected_subs = [sub for sub in fid_subs if sub.subscriber_uid == subscriber_uid]
        assert len(expected_subs) == len(fids), \
            'Expected subscription with subscriber_uid {} and fid {}. Subscriptions: {}'.format(
                subscriber_uid, fids, subs
            )


@then(u'subscriber has new subscription')
@then(u'subscriber has all new subscriptions')
def step_has_new_subscriptions(context):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    owner_fids = context.shared_folders_parents.keys()
    with user_mdb_queries(context, subscriber_uid) as q:
        subs = q.subscribed_folders()
        fid_subs = [sub for sub in subs if sub.owner_fid in owner_fids]
        assert len(fid_subs) >= len(owner_fids), \
            'Expected to have subscribed_folder on owner_fids {}. Subscribed folders: {}'.format(owner_fids, subs)

        expected_subs = [sub for sub in fid_subs if sub.owner_uid == owner_uid]
        assert len(expected_subs) == len(owner_fids), \
            'Expected subscribed folder with owner_uid {} and owner_fids {}. Subscribed folders: {}'.format(
                owner_uid, owner_fids, subs
            )


@then(u'trees hierarchy match')
def step_trees_matches(context):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    shared_fids = context.shared_folders_parents.keys()
    with user_mdb_queries(context, subscriber_uid) as q:
        subs = q.subscribed_folders()
        folders = q.folders()
        for shared_fid in shared_fids:
            expected_parent = context.shared_folders_parents[shared_fid]
            sub_fid = next((s.fid for s in subs if s.owner_fid == shared_fid and s.owner_uid == owner_uid), None)
            sub_parent = next((f.parent_fid for f in folders if f.fid == sub_fid), None)
            shared_parent = next((s.owner_fid for s in subs if s.fid == sub_parent and s.owner_uid == owner_uid), None)
            assert_that(shared_parent, equal_to(expected_parent),
                        'Expected parent_fid {} for shared_fid {}, but got {}'
                        .format(expected_parent, shared_fid, shared_parent))


@then(u'all subscriptions are {state}')
def step_check_subscriptions_state(context, state):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    fids = context.shared_folders_parents.keys()

    with user_mdb_queries(context, owner_uid) as q:

        def get_all_states():
            subs = q.shared_folder_subscriptions()
            found_states = [sub.state for sub in subs if sub.fid in fids and sub.subscriber_uid == subscriber_uid]
            return found_states

        assert_that(get_all_states, with_retries(any_of(only_contains(state), empty())))


@then(u'owner has no subscribers')
def step_owner_has_no_subscribers(context):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    fids = context.shared_folders_parents.keys()

    with user_mdb_queries(context, owner_uid) as q:

        def get_all_subscribers():
            subs = q.shared_folder_subscriptions()
            found_subs = [sub for sub in subs if sub.fid in fids and sub.subscriber_uid == subscriber_uid]
            return found_subs

        assert_that(
            get_all_subscribers,
            with_retries(is_(empty()))
        )


@then(u'subscriber has no subscriptions')
def step_subscriber_has_no_subscriptions(context):
    owner_uid = context.owner_uid
    subscriber_uid = context.subscriber_uid
    owner_fids = context.shared_folders_parents.keys()
    with user_mdb_queries(context, subscriber_uid) as q:
        subs = q.subscribed_folders()
        found_subs = [sub for sub in subs if sub.owner_fid in owner_fids and sub.owner_uid == owner_uid]
        assert_that(found_subs, is_(empty()))


def found_folders_match(context, matcher):
    subscriber_uid = context.subscriber_uid
    subscribed_fids = [s.fid for s in context.subscribed_folders]
    with user_mdb_queries(context, subscriber_uid) as q:
        folders = q.folders()
        found_folders = [f for f in folders if f.fid in subscribed_fids]
        assert_that(found_folders, matcher)


@then(u'subscriber folders were deleted')
def step_folders_deleted(context):
    found_folders_match(context, is_(empty()))


@then(u'subscriber folders were not deleted')
def step_folders_not_deleted(context):
    found_folders_match(context, is_not(empty()))


@then(u'subscriber messages were deleted')
def step_messages_deleted(context):
    subscriber_uid = context.subscriber_uid
    subscription_ids = [s.subscription_id for s in context.subscribed_folders]

    with user_mdb_queries(context, subscriber_uid) as q:
        synced_messages = q.synced_messages()
        found_sm = [sm for sm in synced_messages if sm.subscription_id in subscription_ids]
        assert_that(found_sm, is_(empty()))


def get_full_path(folders, fid):
    res = []
    while True:
        folder = next((f for f in folders if f.fid == fid))
        if not folder:
            break
        res += [folder.name]
        fid = folder.parent_fid
        if not fid:
            break
    res.reverse()
    return res


@then(u'all subscriber folders are imap_unsubscribed')
def step_all_subscribed_folders_imap_unsubscribed(context):
    owner_uid = context.owner_uid
    uid = context.subscriber_uid
    shared_fids = context.shared_folders_parents.keys()
    with user_mdb_queries(context, uid) as q:
        subs = q.subscribed_folders()
        folders = q.folders()

        sub_fids = (s.fid for s in subs if s.owner_fid in shared_fids and s.owner_uid == owner_uid)
        imap_unsubscribed = [i.full_name for i in q.imap_unsubscribed_folders()]
        for sfid in sub_fids:
            assert_that(get_full_path(folders, sfid), is_in(imap_unsubscribed))


@then(u'unsubscribe was successfull')
def step_unsubscribe_successfull(context):
    context.execute_steps(u'''
        Then response code is 200
        And response is ok
        When doberman clears subscriber folders
        Then all subscriptions are terminated
        And subscriber messages were deleted
        And owner has no subscribers
        And subscriber has no subscriptions
    ''')


def get_rules_for_folder(context):
    uid = context.uid
    fid = context.shared_folder_fid
    with user_mdb_queries(context, uid) as q:
        return [r for r in q.folder_archivation_rules() if r.fid == fid]


def get_archivation_rule(context):
    all_rules = get_rules_for_folder(context)
    assert_that(all_rules, has_length(1))
    return all_rules[0]


@then(u'user has archivation rule')
def step_has_archivation_rule(context):
    rule = get_archivation_rule(context)
    if context.table:
        def assert_equal_with_types(real, exp):
            assert_that(real, equal_to(type(real)(exp)))

        expected = context.table[0]
        for key in expected.headings:
            assert_equal_with_types(getattr(rule, key), expected[key])


@then(u'user has no archivation rules')
def step_has_no_archivation_rules(context):
    all_rules = get_rules_for_folder(context)
    assert_that(all_rules, empty())
