import http
import logging
import random
import time

import allure
import requests
from shapely.geometry import Point
from shapely.geometry.polygon import Polygon

from drive.backend.api.client import BackendClient
from drive.backend.api.objects import Error

PROTECTED_TAGS_LIST = ["user_registered_manually", "fake_debt_other_reason", "test_fine"]


def expect_http_code(f, code=http.HTTPStatus.OK):
    try:
        response = f()
        if code != 200:
            raise Exception("expected code should be{}".format(code))
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


@allure.step("Запросить все машины")
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


@allure.step("Найти машину-призрака")
def wait_for_future_car(client, timeout=120):
    car = None
    timeout_start = time.time()
    while time.time() < timeout_start + timeout:
        car = get_car(client, future_car=True)
        if car:
            break
        time.sleep(5)
    assert car, "Future car not found"
    return car


@allure.step("Получить текущую сессию")
def get_current_session(client):
    current_session = None
    for i in range(3):
        current_session = client.current_session()
        code = get_http_code(lambda: current_session)
        if code == 200:
            break
    assert current_session
    return current_session


@allure.step("Закончить поездку и получить чек")
def end_session_and_get_bill(client, user_choice=None):
    session = get_current_session(client)
    session_id = session.id
    end_session(client, user_choice=user_choice)
    bill = get_bill(client, session_id)
    return bill


@allure.step("Получить оффер")
def get_offer(client, car,
              offer_type="standart_offer", user_position=None,
              constructor_id=None, user_destination=None):
    offer = None
    try:
        offers = client.create_offers(car,
                                      user_position=user_position,
                                      user_destination=user_destination,
                                      offer_name=constructor_id)
        assert len(offers) > 0, "no offers found"
        offer = list(filter(lambda offer: offer.type == offer_type, offers))[0]
        assert offer, f'no offer found type:{offer_type}'
        logging.info("selected offer is {}".format(offer.id))
    except:
        Exception("Something wrong during get offers")
    if not offer:
        Exception("Something wrong during get offers")
    return offer


@allure.step("Получить машину")
def get_car(client, cars=None, future_car=False):
    if not cars:
        cars = get_cars(client)
    selected = None
    if future_car:
        cars_list_filtred = list(filter(lambda car: car.number and not car.location, cars))
    else:
        cars_list_filtred = list(filter(lambda car: car.number and car.location, cars))
    if cars_list_filtred:
        selected = random.choice(cars_list_filtred)
        selected.id = get_car_id(client, selected)
        assert selected
        if is_allow_drop_car(client, selected):
            logging.info("selected car is " + selected.number)
            return selected
    if not selected:
        logging.info("There are no cars available at this moment")
    return selected


@allure.step("Получить машину по номеру")
def get_car_by_number(client, cars, car_number):
    for car in cars:
        if car.number == car_number:
            car.id = get_car_id(client, car)
            return car
    logging.info(f'car {car_number} not found')
    return None


@allure.step("Проверить находится ли машина в зоне завершения аренды")
def is_allow_drop_car(client, car):
    car_details = client.car_details(car, by="number")
    if car_details.cargo_allow_drop_car:
        return car_details.cargo_deny_drop_car
    return car_details.is_allow_drop


def get_car_id(client, car):
    cars = client.search_cars(car.number)
    return cars[0].id


def get_bill(client, session_id):
    session = client.get_session(session_id)
    return session.bill


@allure.step("Установить новое местоположение машины")
def set_car_location(client, car, location):
    lon = float(location.split()[0])
    lat = float(location.split()[1])
    client.set_parameter(car, id=101, value=lat)
    client.set_parameter(car, id=102, value=lon)
    time.sleep(20)
    car_lat = client.get_car_info(car).location.get("lat")
    car_lon = client.get_car_info(car).location.get("lon")
    logging.info(f'car location is {car_lat} {car_lon}')


@allure.step("Установить новый пробег на машине")
def set_mileage(client, car, mileage):
    client.set_parameter(car, id=2103, value=mileage)
    time.sleep(60)
    get_mileage(client, car)


@allure.step("Получить пробег")
def get_mileage(client, car):
    mileage = client.get_parameter(car, id=2103)
    logging.info(f'current mileage is {mileage}')
    return mileage


@allure.step("Забронировать машину")
def reservation(client, offer):
    for i in range(3):
        code = get_http_code(lambda: client.accept_offer(offer))
        if code == 200 or get_current_session(client).current_performing == "old_state_reservation":
            return
    raise Exception("failed transition to reservation state")


@allure.step("Перевести аренду в осмотр")
def acceptance(client):
    for i in range(3):
        code = get_http_code(lambda: client.start_session())
        current_session = get_current_session(client)
        if code == 200 or current_session.current_performing == "old_state_acceptance":
            return
    raise Exception("failed transition to acceptance state")


@allure.step("Перевести аренду в поездку")
def riding(client):
    for i in range(3):
        code = get_http_code(lambda: client.evolve_to_riding())
        current_session = get_current_session(client)
        if code == 200 or current_session.current_performing == "old_state_riding":
            return
    raise Exception("failed transition to riding state")


@allure.step("Перевести аренду в паркинг")
def parking(client):
    for i in range(3):
        code = get_http_code(lambda: client.evolve_to_parking())
        current_session = get_current_session(client)
        if code == 200 or current_session.current_performing == "old_state_parking":
            return
    raise Exception("failed transition to parking state")


@allure.step("Завершить аренду")
def end_session(client, error_code=200, user_choice=None):
    for i in range(3):
        code = get_http_code(lambda: client.end_session(user_choice=user_choice))
        current_session = get_current_session(client)
        if code == error_code or current_session.is_finished:
            return
    raise Exception("failed transition to end session")


@allure.step("Завершить аренду с ошибкой")
def end_session_with_failure(client):
    response = expect_http_code(lambda: client.end_session(), 400)
    assert response
    return Error.from_json(response.get("error_details"))


@allure.step("Прогрев")
def heating(client):
    expect_http_code(lambda: client.action_heating(), 200)


def create_model_list(client):
    cars = get_cars(client)
    model_list = set()
    for car in cars:
        model_list.add(car.model)
    return model_list


@allure.step("Получить теги машины")
def get_car_tags(client, car):
    car_info = None
    for i in range(3):
        car_info = client.get_car_info(car)
        code = get_http_code(lambda: car_info)
        if code == 200:
            break
    return car_info.tags


@allure.step("Создать долг")
def create_debt(client):
    for i in range(3):
        code = get_http_code(lambda: client.add_user_tag(tag_name="test_fine", amount=109900))
        if code == http.HTTPStatus.OK:
            return
    raise Exception("can't create fine")


@allure.step("Заблокировать пользователя")
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


@allure.step("Разблокировать пользователя")
def unblock_user(client):
    block_tag_id = get_tag_id(client, tag_name="blocked_by_security")
    if block_tag_id:
        client.remove_user_tag(block_tag_id)


@allure.step("Проверить статус долга у пользователя")
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


@allure.step("Снять долг")
def remove_debt(client, timeout=60):
    payment_cancellation(client)
    timeout_start = time.time()
    while time.time() < timeout_start + timeout:
        debt = check_debt(client)
        if not debt:
            break
        time.sleep(5)


@allure.step("Получить список ролей")
def get_role_list(client):
    role_list = client.get_user_roles()
    return role_list


@allure.step("Установить роли")
def set_roles(client, enable_roles_list=[], disable_roles_list=[]):
    # Attention: Roles which will not be specified will be removed from user
    roles_list = get_role_list(client)
    remove_roles = [role.role_id for role in roles_list if
                    role not in enable_roles_list and role not in disable_roles_list]
    for role in remove_roles:
        client.remove_user_role(role)
    for role in disable_roles_list:
        try:
            client.add_user_role(role_id=role, activate=0)
            client.deactivate_role(role_id=role)
            logging.info(f'{role} added and disabled')
        except:
            logging.error(f'something wrong with {role}')
    for role in enable_roles_list:
        try:
            client.add_user_role(role_id=role, activate=1)
            client.activate_role(role_id=role)
            logging.info(f'{role} added and enabled')
        except:
            logging.error(f'something wrong with {role}')
    time.sleep(3)


def is_polygon_contains(polygon_coords=(), point=()):
    polygon = Polygon(polygon_coords)
    pt = Point(point)
    return polygon.contains(pt)


@allure.step("Удалить теги пользователя")
def remove_all_users_tag(client, protected_tags_list=None):
    tags = client.get_user_tags()
    for tag in tags:
        tag_id = tag.tag_id
        tag_name = tag.name
        if tag_name not in protected_tags_list:
            client.remove_user_tag(tag_id)


@allure.step("Добавить бонусов")
def add_bonuses(client, amount):
    client.add_user_tag(tag_name="test_bb_credit", amount=amount)
    logging.info(f'{amount} bonuses added successfully')


@allure.step("Получить кол-во бонусов")
def get_bonuses_amount(client):
    bonuses = get_current_session(client).user.bonuses_amount
    return bonuses


@allure.step("Залочить машину пользователем")
def lock_car(client, car=None):
    for i in range(0, 3):
        try:
            car = car if car else get_car(client)
            car_tags = client.list_tags(car)
            tag_id = None
            for tag in car_tags:
                if tag.name == "autotest_tag":
                    tag_id = tag.tag_id
                    break
            if tag_id:
                client.start_servicing(tag_id=tag_id, car_id=car.id)
                time.sleep(3)
                break
        except BaseException as err:
            logging.error(err)
            continue


@allure.step("разблокировать машину")
def unlock_car(client):
    car_tags = client.get_tags_by_performer()
    tag_id = None
    car_id = None
    for tag in car_tags:
        if tag.name == "autotest_tag":
            tag_id = tag.tag_id
            car_id = tag.object_id
            break
    if tag_id:
        client.finish_servicing(tag_id=tag_id, car_id=car_id, drop=True)


@allure.step("Установить пользователю начальное окружения")
def client_initial_state(client: BackendClient, min_bonuses_amount=1000000):
    session = get_current_session(client)
    remove_all_users_tag(client, protected_tags_list=PROTECTED_TAGS_LIST)
    bonuses = session.user.bonuses_amount
    if bonuses < min_bonuses_amount:
        add_bonuses(client, amount=10000000)
    if not session.is_finished:
        client.drop_session()
        session = get_current_session(client)
    assert session.is_finished
    debt = check_debt(client)
    if debt:
        remove_debt(client)
        debt = check_debt(client)
    assert not debt
    client_info = client.get_user_info()
    client.register_user_force()
    client.edit_user(username=client_info.username,
                     status="active",
                     first_name=client_info.first_name,
                     last_name=client_info.last_name,
                     uid=client_info.uid,
                     email=client_info.email,
                     is_phone_verified=True,
                     phone='+91111111111')
    set_roles(client,
              enable_roles_list=['autotest_role'],
              disable_roles_list=['GR_default_user_base', 'user_access_base'])
    unlock_car(client)
