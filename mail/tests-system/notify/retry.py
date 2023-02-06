# from pycommon import *
# from subscriber import *
# import time

# retry_start_interval = 1
# retry_backoff_coefficient = 2
# retry_end_interval = 16
# wakeup_interval = 2

# def setUp():
#     global hub
#     global dead_subscriber
#     global good_subscriber
#     hub = Client(Testing.host(), Testing.port())
#     hub.unsubscribe_all(Testing.uid1(), "mail")
#     hub.unsubscribe_all(Testing.uid2(), "mail")
#     hub.unsubscribe_all(Testing.uid1(), Testing.service1())
#     hub.unsubscribe_all(Testing.uid2(), Testing.service1())
#     hub.unsubscribe_all(Testing.uid1(), Testing.service2())
#     hub.unsubscribe_all(Testing.uid2(), Testing.service2())
#     hub.xtasks_clear()

#     dead_subscriber = Subscriber(Testing.dead_subscriber_port())
#     dead_subscriber.start()

#     good_subscriber = Subscriber(Testing.subscriber_port())
#     good_subscriber.start()

# def tearDown():
#     hub.xtasks_clear()
#     dead_subscriber.stop()
#     good_subscriber.stop()

# def assert_sub_in_retry(sub_list, id):
#     subs = [s for s in sub_list if s['id'] == id]
#     assert_equals(len(subs), 1)
#     assert_greater(subs[0]['retry_interval'], 0)
#     assert_greater(subs[0]['next_retry_time'], 0)
#     return subs[0]

# def assert_sub_not_in_retry(sub_list, id):
#     subs = [s for s in sub_list if s['id'] == id]
#     assert_equals(len(subs), 1)
#     assert_equals(subs[0]['retry_interval'], 0)
#     assert_equals(subs[0]['next_retry_time'], 0)
#     return subs[0]

# def setup_subscribers(dead, good, **sub_params):
#     global dead_subid
#     global good_subid

#     if dead is not None:
#         dead.set_response(500, raw_response="KO")
#         dead_subid = hub.subscribe(Testing.uid1(), Testing.service1(), dead.url, **sub_params)

#     if good is not None:
#         good.set_response(200, raw_response="OK")
#         good_subid = hub.subscribe(Testing.uid1(), Testing.service1(), good.url, **sub_params)

#     subs = hub.list(Testing.uid1(), Testing.service1())

#     if dead is not None:
#         assert_sub_not_in_retry(subs, dead_subid)

#     if good is not None:
#         assert_sub_not_in_retry(subs, good_subid)

# def list_subs():
#     return hub.list(Testing.uid1(), Testing.service1())

# class TestNotifyRetry:
#     def setUp(self):
#         self.teardown()

#     def teardown(self):
#         dead_subscriber.messages = []
#         good_subscriber.messages = []
#         hub.unsubscribe_all(Testing.uid1(), Testing.service1())
#         hub.unsubscribe_all(Testing.uid1(), Testing.service2())
#         hub.unsubscribe_all(Testing.uid2(), Testing.service1())
#         hub.unsubscribe_all(Testing.uid2(), Testing.service2())
#         hub.xtasks_clear()

#     def test_single_retry_single_subscription(self):
#         setup_subscribers(dead_subscriber, None)

#         time.sleep(1.0) # otherwise message event_ts can be less than sub init_time

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         time.sleep(0.8 * retry_start_interval)
#         assert_equals(len(dead_subscriber.messages), 1)
#         assert_sub_in_retry(list_subs(), dead_subid)

#         dead_subscriber.set_response(200, raw_response="OK")
#         time.sleep(0.2 * retry_start_interval + 4 * wakeup_interval)
#         assert_equals(len(dead_subscriber.messages), 2)
#         assert_sub_not_in_retry(list_subs(), dead_subid)

#     def test_single_retry_multiple_subscriptions(self):
#         setup_subscribers(dead_subscriber, good_subscriber)

#         time.sleep(1.0)

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         time.sleep(0.5 * retry_start_interval)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 1)
#         assert_sub_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 1)
#         assert_sub_not_in_retry(sub_list, good_subid)

#         dead_subscriber.set_response(200, raw_response="OK")
#         time.sleep(0.5 * retry_start_interval + 2 * wakeup_interval)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 2)
#         assert_sub_not_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 1)
#         assert_sub_not_in_retry(sub_list, good_subid)

#     def test_single_retry_from_nonzero_ack_local_id(self):
#         setup_subscribers(dead_subscriber, good_subscriber)
#         dead_subscriber.set_response(200, raw_response="OK")

#         time.sleep(1.0)

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         time.sleep(0.5)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 1)
#         assert_sub_not_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 1)
#         assert_sub_not_in_retry(sub_list, good_subid)

#         dead_subscriber.set_response(500, raw_response="KO")

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         time.sleep(0.8 * retry_start_interval)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 2)
#         assert_sub_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 2)
#         assert_sub_not_in_retry(sub_list, good_subid)

#         dead_subscriber.set_response(200, raw_response="OK")
#         time.sleep(0.2 * retry_start_interval + 3 * wakeup_interval)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 3)
#         assert_sub_not_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 2)
#         assert_sub_not_in_retry(sub_list, good_subid)

#     def test_retry_when_xstore_read_limit_is_exceeded(self):
#         setup_subscribers(dead_subscriber, good_subscriber)
#         dead_subscriber.set_response(200, raw_response="OK")

#         time.sleep(1.0)

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         time.sleep(0.5)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 1)
#         assert_sub_not_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 1)
#         assert_sub_not_in_retry(sub_list, good_subid)

#         dead_subscriber.set_response(500, raw_response="KO")

#         messages = []
#         for i in range(0,10):
#             m = 'test_data' + str(i);
#             messages.append(m)
#             hub.notify(Testing.uid1(), Testing.service1(), m)

#         time.sleep(1.0)
#         sub_list = list_subs()
#         assert_equals(len(dead_subscriber.messages), 2)
#         assert_sub_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 11)
#         assert_sub_not_in_retry(sub_list, good_subid)

#         # make sure that the task is not recreated continiously
#         tasks = hub.xtasks_summary()
#         assert_equals(len(tasks['pending']), 0)
#         assert_equals(len(tasks['active']), 0)
#         assert_equals(len(tasks['delayed']), 1)

#         dead_subscriber.set_response(200, raw_response="OK")
#         time.sleep(2 * retry_start_interval + 2 * wakeup_interval)
#         sub_list = list_subs()

#         assert_equals(len(dead_subscriber.messages), 12)
#         assert_equals(dead_subscriber.messages[0][7], 'test_data')
#         assert_equals(dead_subscriber.messages[1][7], 'test_data0')
#         for i in range(0,10):
#             assert_equals(dead_subscriber.messages[i + 2][7], messages[i])
#         assert_sub_not_in_retry(sub_list, dead_subid)
#         assert_equals(len(good_subscriber.messages), 11)
#         assert_sub_not_in_retry(sub_list, good_subid)

#     def test_retry_ttl_expired(self):
#         setup_subscribers(dead_subscriber, None)

#         time.sleep(1.0) # otherwise message event_ts can be less than sub init_time

#         transit_id = 'transit_' + str(time.time())
#         ttl = retry_start_interval

#         hub.binary_notify(make_message(Testing.uid1(), Testing.service1(),
#             {"payload" : "test_data"}, '{"payload":"test_data"}',
#             transit_id, 0,0, '', tags=['a'], ttl=ttl))

#         time.sleep(0.8 * retry_start_interval)
#         assert_equals(len(dead_subscriber.messages), 1)
#         assert_sub_in_retry(list_subs(), dead_subid)

#         # wait until next retry attempt, ack_local_id should get updated,
#         # retry state retained because there were no attempts to deliver anything
#         time.sleep(2 * retry_start_interval + 1)
#         sub = assert_sub_in_retry(list_subs(), dead_subid)
#         assert_greater(sub['ack_local_id'], 0)

#     def test_retry_all_messages_filtered(self):
#         setup_subscribers(dead_subscriber, None,
#             filter='{"rules": [{ "if": { "$has_tags": ["a"] }, "do": "send_bright" },{ "do": "skip" }], "vars": {}}')

#         time.sleep(1.0)

#         # put sub in retry by sending a message which will not be filtered
#         # but has a low ttl, so will be discarded on retry
#         hub.binary_notify(make_message(Testing.uid1(), Testing.service1(),
#             {"payload" : "test_data"}, '{"payload":"test_data"}',
#             'transit2_' + str(time.time()), 0,0, '', tags=['a'], ttl=retry_start_interval))

#         time.sleep(0.8 * retry_start_interval)
#         assert_equals(len(dead_subscriber.messages), 1)
#         assert_sub_in_retry(list_subs(), dead_subid)

#         # wait until next retry attempt, ack_local_id should get updated,
#         # retry state retained because there were no attempts to deliver anything
#         time.sleep(0.2 * retry_start_interval + 2 * wakeup_interval)
#         sub = assert_sub_in_retry(list_subs(), dead_subid)
#         assert_greater(sub['ack_local_id'], 0)
#         pre_filter_ack_local_id = sub['ack_local_id']

#         # now, when sub is in retry, send another message, which gets filtered
#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         # wait until next retry attempt, ack_local_id should get updated,
#         # retry state retained because there were no attempts to deliver anything
#         time.sleep(retry_start_interval + 2 * wakeup_interval)
#         sub = assert_sub_in_retry(list_subs(), dead_subid)
#         assert_greater(sub['ack_local_id'], pre_filter_ack_local_id)

#     def test_retry_new_messages(self):
#         setup_subscribers(dead_subscriber, None)

#         time.sleep(0.2)

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         time.sleep(0.8 * retry_start_interval)
#         assert_equals(len(dead_subscriber.messages), 1)

#         assert_sub_in_retry(list_subs(), dead_subid)

#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')
#         hub.notify(Testing.uid1(), Testing.service1(), 'test_data')

#         dead_subscriber.set_response(200, raw_response="OK")

#         time.sleep(0.2 * retry_start_interval + 4 * wakeup_interval)

#         assert_equals(len(dead_subscriber.messages), 4)
#         assert_sub_not_in_retry(list_subs(), dead_subid)
