import http
import logging
import time

import requests
from shapely.geometry import Point
from shapely.geometry.polygon import Polygon

from drive.backend.api.objects import Error

PROTECTED_TAGS_LIST = ["user_registered_manually", "fake_debt_other_reason", "test_fine"]

def expect_http_code(f, code=http.HTTPStatus.OK):
    try:
        response = f()
        if code != 200 and response:
            raise Exception("expected code should be{}".format(response.status_code))
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == code:
            logging.info("got expected code {}".format(code))
            return e.response.json()
        else:
            raise Exception("got code {}, but expected {}".format(e.response.status_code, code))


def get_http_code(f):
    try:
        f()
        return http.HTTPStatus.OK
    except requests.exceptions.HTTPError as e:
        return e.response.status_code


def get_cars(client):
    cars = None
    for i in range(3):
        cars = client.list_cars_user()
        code = get_http_code(lambda: cars)
        if code == http.HTTPStatus.OK:
            break
    if not len(cars):
        logging.info("car list is empty")
    return cars


def wait_for_future_car(client):
    car = None
    while True:
        cars = get_cars(client)
        car = get_car(client, cars, future_car=True)
        if car:
            break
    assert car, "Future car not found"
    return car


def get_current_session(client):
    current_session = None
    for i in range(3):
        current_session = client.current_session()
        code = get_http_code(lambda: current_session)
        if code == 200:
            break
    assert current_session
    return current_session


def end_session_and_get_bill(client, user_choice=None):
    session = get_current_session(client)
    session_id = session.id
    end_session(client, user_choice=user_choice)
    bill = get_bill(client, session_id)
    return bill


def get_offer(client, car, offer_type="standart_offer",
              user_position=None, destination_name=None,
              constructor_id=None, user_destination=None):
    offers = client.create_offers(car, user_position,
                                  destination_name, user_destination=user_destination,
                                  offer_name=constructor_id)
    assert len(offers) > 0, "no offers found"

    offer = list(filter(lambda offer: offer.type == offer_type, offers))[0]
    assert offer, f'no offer found type:{offer_type}'

    assert car.number == offer.car_info.number
    logging.info("selected offer is {}".format(offer.id))
    return offer


def get_car(client, cars, car_model_id=None, future_car=False):
    selected = None
    cars_list_filtred = list(filter(lambda car: car.number, cars))
    if not future_car:
        cars_list_filtred = list(filter(lambda car: car.number and car.location, cars))
    if car_model_id:
        logging.info("serching " + car_model_id)
        cars_list_filtred = list(filter(lambda car: car.model == car_model_id and car.number, cars_list_filtred))
    if cars_list_filtred:
        for car in cars_list_filtred:
            selected = car
            selected.id = get_car_id(client, selected)
            assert selected
            if is_allow_drop_car(client, selected):
                logging.info("selected car is " + selected.number)
                return selected
    if not selected:
        logging.info("There are no cars available at this moment")
    return selected


def get_car_by_number(client, cars, car_number):
    for car in cars:
        if car.number == car_number:
            car.id = get_car_id(client, car)
            return car
    logging.info(f'car {car_number} not found')
    return None


def is_allow_drop_car(client, car):
    car_details = client.car_details(car)
    if car_details.cargo_allow_drop_car:
        return car_details.cargo_deny_drop_car
    return car_details.is_allow_drop


def get_car_id(client, car):
    cars = client.list_cars()
    car = list(filter(lambda x: x.number == car.number, cars))
    return car[0].id


def get_bill(client, session_id):
    session = client.get_session(session_id)
    return session.bill


def set_car_location(client, car, location):
    lon = float(location.split()[0])
    lat = float(location.split()[1])
    client.set_parameter(car, id=101, value=lat)
    client.set_parameter(car, id=102, value=lon)
    time.sleep(20)


def set_mileage(client, car, mileage):
    client.set_parameter(car, id=2103, value=mileage)
    time.sleep(60)
    get_mileage(client, car)


def get_mileage(client, car):
    mileage = client.get_parameter(car, id=2103)
    logging.info(f'current mileage is {mileage}')
    return mileage


def reservation(client, offer):
    for i in range(3):
        code = get_http_code(lambda: client.accept_offer(offer))
        if code == 200 or get_current_session(client).current_performing == "old_state_reservation":
            return
    raise Exception("failed transition to reservation state")


def acceptance(client):
    for i in range(3):
        code = get_http_code(lambda: client.start_session())
        current_session = get_current_session(client)
        if code == 200 or current_session.current_performing == "old_state_acceptance":
            return
    raise Exception("failed transition to acceptance state")


def riding(client):
    for i in range(3):
        code = get_http_code(lambda: client.evolve_to_riding())
        current_session = get_current_session(client)
        if code == 200 or current_session.current_performing == "old_state_riding":
            return
    raise Exception("failed transition to riding state")


def parking(client):
    for i in range(3):
        code = get_http_code(lambda: client.evolve_to_parking())
        current_session = get_current_session(client)
        if code == 200 or current_session.current_performing == "old_state_parking":
            return
    raise Exception("failed transition to parking state")


def end_session(client, error_code=200, user_choice=None):
    for i in range(3):
        code = get_http_code(lambda: client.end_session(user_choice=user_choice))
        current_session = get_current_session(client)
        if code == error_code or current_session.is_finished:
            return
    raise Exception("failed transition to end session")


def end_session_with_failure(client):
    response = expect_http_code(lambda: client.end_session(), 400)
    assert response
    return Error.from_json(response.get("error_details"))


def heating(client):
    expect_http_code(lambda: client.action_heating(), 200)


def create_model_list(client):
    cars = get_cars(client)
    model_list = set()
    for car in cars:
        model_list.add(car.model)
    return model_list


def get_car_tags(client, car):
    car_info = None
    for i in range(3):
        car_info = client.get_car_info(car)
        code = get_http_code(lambda: car_info)
        if code == 200:
            break
    return car_info.tags


def create_debt(client):
    for i in range(3):
        code = get_http_code(lambda: client.add_user_tag(tag_name="test_fine", amount=109900))
        if code == http.HTTPStatus.OK:
            return
    raise Exception("can't create fine")


def block_user(client):
    for i in range(3):
        code = get_http_code(lambda: client.add_user_tag(tag_name="blocked_by_security"))
        if code == http.HTTPStatus.OK:
            return
    raise Exception("can't block user")


def get_tag_id(client, tag_name):
    tags_list = client.get_user_tags()
    tag_id = None
    for tag in tags_list:
        if tag.name == tag_name:
            tag_id = tag.tag_id
    return tag_id


def unblock_user(client):
    block_tag_id = get_tag_id(client, tag_name="blocked_by_security")
    if block_tag_id:
        client.remove_user_tag(block_tag_id)


def check_debt(client):
    current_session = get_current_session(client)
    return current_session.user.debt_amount > 0


def get_current_payment_tickets(client) -> []:
    inprogress_payment_states = ["finishing", "canceled"]
    session_ids = []
    payments_current = client.get_payment_info().current
    if payments_current:
        for payment in payments_current:
            if payment.state not in inprogress_payment_states:
                session_ids.append(payment.session_id)
    return session_ids


def payment_cancellation(client):
    tickets = get_current_payment_tickets(client)
    if tickets:
        for ticket in tickets:
            client.add_user_tag(tag_name='fake_debt_other_reason', session_id=ticket)


def remove_debt(client, timeout=600):
    payment_cancellation(client)
    while True:
        debt = check_debt(client)
        if not debt:
            break
        time.sleep(5)

def get_role_list(client):
    role_list = client.get_user_roles()
    roles = {role.role_id for role in role_list}
    return role_list


def enable_roles(client, *roles):
    roles_list = get_role_list(client)
    for role in roles_list:
        if role.role_id in roles and role.active:
            logging.info(f'{role.role_id} already enabled')
        if role.role_id not in roles:
            disable_roles(client, role.role_id)
            logging.info(f'{role.role_id} disabled')
    roles_ids = [role.role_id for role in roles_list]
    for role in roles:
        if role not in roles_ids:
            client.add_role(role)
        client.activate_role(role)
        logging.info(f'{role} activated')


def disable_roles(client, *roles):
    for role in roles:
        client.deactivate_role(role)


def is_polygon_contains(polygon_coords=(), point=()):
    polygon = Polygon(polygon_coords)
    pt = Point(point)
    return polygon.contains(pt)


def remove_all_users_tag(client, protected_tags_list=None):
    tags = client.get_user_tags()
    for tag in tags:
        tag_id = tag.tag_id
        tag_name = tag.name
        if tag_name not in protected_tags_list:
            client.remove_user_tag(tag_id)


def add_bonuses(client, amount):
    client.add_user_tag(tag_name="test_bb_credit", amount=amount)
    logging.info(f'{amount} bonuses added successfully')

def get_bonuses_amount(client):
    bonuses = get_current_session(client).user.bonuses_amount
    return bonuses

def client_initial_state(client, min_bonuses_amount=1000000):
    remove_all_users_tag(client, protected_tags_list=PROTECTED_TAGS_LIST)

    session = get_current_session(client)
    bonuses = session.user.bonuses_amount
    debt = check_debt(client)

    if bonuses < min_bonuses_amount:
        add_bonuses(client, amount=10000000)
    if not session.is_finished:
        client.drop_session()
    session = get_current_session(client)
    assert session.is_finished

    if debt:
        remove_debt(client)
        debt = check_debt(client)
        assert not debt
    if session.user.status != "active":
        client_info = client.get_user_info()
        client.edit_user(username=client_info.username,
                         status="active",
                         first_name=client_info.first_name,
                         last_name=client_info.last_name,
                         uid=client_info.uid,
                         email=client_info.email)
        status = get_current_session(client).user.status
        assert status == "active"
