# $ ya tool tvmknife unittest service --src=1000509 --dst=1000510
SERVICE_TICKET = '3:serv:CBAQ__________9_IggIvYg9EL6IPQ:UJUgt_WegtDqKmmrLmPTH2de5GPD2jX0tdfNjqlN2MeL4JAT-iztrkiUjNN9fulzIUsxVAKZlfoCaaJOd9zvkCiqCuzHQFTXItU_Kxc4XhRbWSHUc99u9MY9lA2b85QcedAzNDdCo8O-KYAq1EtEj2Ao1euLkIdc7Efcdy-AIdY'  # noqa

# $ ya tool tvmknife unittest user --default 123456 --env prod
USER_TICKET = '3:user:CAsQ__________9_GhIKBAjAxAcQwMQHINKF2MwEKAA:BGaqxMq46DctHCN7KLkdriUchNowVLf9FImBrE2zOZc3DFoeaS3aP1p4LTybV8iCyeZsb3XUAgMRd54wRBL_f--KtApQuDwi1LhPR8qtEULE3ksOLu4oqWcv8o6vI-gNvepDqXyS-Jt9CCnu8OsNIRx7r7KD0Z_U1QYSGr60Quk'  # noqa


def test_tvm_service_tickets(upper):
    upper.set_query(ms='pb', text='минск')

    r = upper.get_raw()
    assert r.status_code == 200

    r = upper.get_raw(tvm=1)
    assert r.status_code == 401

    upper.options.headers['X-Ya-Service-Ticket'] = '3:serv:malformed_ticket'
    r = upper.get_raw()
    assert r.status_code == 401

    upper.options.headers['X-Ya-Service-Ticket'] = SERVICE_TICKET
    r = upper.get_raw(tvm=1)
    assert r.status_code == 200


def test_tvm_user_tickets(upper):
    # set explicit origin that has reqans turned on
    upper.set_query(ms='pb', text='минск', origin='maps-form')

    upper.options.headers['X-Ya-User-Ticket'] = '3:user:malformed_ticket'
    r = upper.get_raw()
    assert r.status_code == 401

    upper.options.headers['X-Ya-Service-Ticket'] = SERVICE_TICKET
    upper.options.headers['X-Ya-User-Ticket'] = '3:user:malformed_ticket'
    r = upper.get_raw()
    assert r.status_code == 401

    reqans1 = upper.list_reqans_records()

    upper.options.headers['X-Ya-Service-Ticket'] = SERVICE_TICKET
    upper.options.headers['X-Ya-User-Ticket'] = USER_TICKET
    r = upper.get_raw()
    assert r.status_code == 200

    reqans2 = upper.list_reqans_records()
    assert len(reqans1) + 1 == len(reqans2)
    assert reqans2[-1].request.get('passport_uid') == '123456'


def test_tvm_on_meta_v2(upper):
    upper.options.auto_add_origin = False
    upper.set_query(ms='pb', origin='test', business_oid=130632855)

    r = upper.get_raw('search')
    assert r.status_code == 200

    r = upper.get_raw('search', origin='non_whitelisted_origin')
    assert r.status_code == 401

    r = upper.get_raw('search', tvm=1)
    assert r.status_code == 401

    upper.options.headers['X-Ya-Service-Ticket'] = '3:serv:malformed_ticket'
    r = upper.get_raw('search')
    assert r.status_code == 401

    upper.options.headers['X-Ya-Service-Ticket'] = SERVICE_TICKET
    r = upper.get_raw('search', tvm=1)
    assert r.status_code == 200
