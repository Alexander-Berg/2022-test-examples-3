from pytest_bdd import scenarios
from .conftest import get_path

from hamcrest import (
    assert_that,
    has_item,
    has_entry,
    has_entries,
    has_length,
    empty,
)
from tests_common.pytest_bdd import (
    when,
    then
)


scenarios(
    "tabs.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/tabs.feature"),
    strict_gherkin=False
)


def get_tab_from_row(row):
    res = {}
    for col in row.headings:
        if col == 'type':
            res[col] = row[col]
        elif col == 'isUnvisited':
            res[col] = (row[col] == 'True')
        else:
            res[col] = int(row[col])
    return res


@then(u'there are "{count:d}" tabs in response')
def step_check_response_length(context, count):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'tabs', has_length(count)))

    if context.table:
        resp = resp['tabs']
        for row in context.table:
            assert_that(resp, has_item(
                has_entries(get_tab_from_row(row))))


@when(u'we request "folder_tabs_new_counters" with "{limit:d}" limit')
def step_request_hound(context, limit):
    context.params['limit'] = limit
    context.execute_steps(u'When we request "folder_tabs_new_counters"')


@then(u'"folder_tabs_new_counters" response with tabs')
@then(u'"folder_tabs_new_counters" response is empty')
def step_check_response_folder_tabs_new_counters(context):
    resp = context.response.json()

    if context.table:
        expected = {}
        for row in context.table:
            expected[row['type']] = int(row['unreadMessagesCount'])
        matcher = has_entries(expected)
    else:
        matcher = empty()

    assert_that(resp, has_entry(
        'new_counters', matcher))
