from behave import then


@then(u'response code is {expect:d}')
def check_response_code(context, expect):
    got = context.req.status_code
    assert got == expect, 'Expect %d, got %r %s' % (expect, context.req, context.req.json())


@then(u'response has "{tag:w}" with value "{val}"')
def check_response_value(context, tag, val):
    obj = context.req.json()
    assert obj[tag] == val, 'Expect %s, got %s' % (val, obj[tag])


@then(u'response has "{tag:w}" with value containing "{val}"')
def check_response_contains(context, tag, val):
    obj = context.req.json()
    assert val in obj[tag], 'Expect to contain %s, got %s' % (val, obj[tag])


@then(u'response has "{tag:w}" with numeric value')
def check_response_numeric_value(context, tag):
    obj = context.req.json()
    assert obj[tag].isdigit(), 'Expect numeric, got %s' % (obj[tag])
    context.shared_folder_fid = int(obj[tag])


@then(u'response is ok')
def check_response_ok(context):
    obj = context.req.json()
    assert obj['ok'], 'Expect {"ok":true}, got %s' % (obj)


@then(u'shared folder fid is same')
def check_same_shared_folder_fid(context):
    obj = context.req.json()
    assert int(obj['shared_folder_fid']) == context.shared_folder_fid, \
        'Shared folder had fid %s, and now has fid %s' % (context.shared_folder_fid, int(obj['shared_folder_fid']))
