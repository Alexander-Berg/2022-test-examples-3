def make_rule(addr_from, action, data=None):
    rule = {
        "rules": [
            {
                "scope": {
                    "direction": "inbound"
                },
                "terminal": True,
                "condition": {"address:from": addr_from},
                "actions": [
                    dict(
                        action=action,
                        **(dict(data=data) if data else {})
                    )
                ]
            }
        ]
    }
    return rule


def make_drop_rule(addr_from):
    return make_rule(addr_from, action='drop')


def make_forward_rule(addr_from, addr_to):
    return make_rule(addr_from, action='forward', data={'email': addr_to})


def check_rule_created(furitadb, expected, sender, org_id):
    expected['rules'][0]['condition_query'] = 'hdr_from_email:{}'.format(sender)

    result = furitadb.query("SELECT org_id, rules FROM furita.domain_rules")
    assert len(result) == 1
    assert result[0] == (org_id, expected)
