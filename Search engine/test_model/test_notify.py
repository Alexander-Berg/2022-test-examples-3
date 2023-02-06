import uuid

from search.martylib.core.date_utils import now
from search.martylib.db_utils import session_scope, to_model

from search.morty.proto.structures import event_pb2, component_pb2, notification_pb2, process_pb2, common_pb2, executor_pb2
from search.morty.sqla.morty import model

from search.morty.src.model.notifications.notify import NotifySender

from search.morty.tests.utils.test_case import MortyTestCase


class TestNotifySender(MortyTestCase):
    def test_set_event(self):
        sender = NotifySender()

        event_2 = event_pb2.Event(id=str(uuid.uuid4()), process=process_pb2.Process(id=str(uuid.uuid4())))
        event_3 = event_pb2.Event(
            id=str(uuid.uuid4()), process=process_pb2.Process(id=str(uuid.uuid4()), subprocesses=[process_pb2.SubProcess(id=str(uuid.uuid4()))])
        )
        with session_scope() as session:
            session.merge(to_model(event_2))
            session.merge(to_model(event_3))
            session.commit()

            n = notification_pb2.Notification(subprocess=event_3.process.subprocesses[0].id)
            sender.set_event(session, [n])
            assert n.event_id == event_3.id

            n = notification_pb2.Notification(subprocess=str(uuid.uuid4()))
            sender.set_event(session, [n])
            assert n.event_id == ''

            n = notification_pb2.Notification(process=event_2.process.id)
            sender.set_event(session, [n])
            assert n.event_id == event_2.id

            n = notification_pb2.Notification(process=str(uuid.uuid4()))
            sender.set_event(session, [n])
            assert n.event_id == ''

    def test_select_notifications(self):
        notification = notification_pb2.Notification(
            id=str(uuid.uuid4()),
            type=notification_pb2.Notification.Type.EXECUTION_STARTS,
            event_id=str(uuid.uuid4()),
            message='test',
        )
        sender = NotifySender()

        # test send_at
        with session_scope() as session:
            notification.send_at = int(now().timestamp()) + 10
            session.merge(to_model(notification))

            assert len(list(sender.select_notifications(session))) == 0

        # test send_at
        with session_scope() as session:
            notification.send_at = 10
            session.merge(to_model(notification))

            assert len(list(sender.select_notifications(session))) == 1

        # test status
        with session_scope() as session:
            notification.status = notification_pb2.Notification.Status.DELIVERED
            session.merge(to_model(notification))

            assert len(list(sender.select_notifications(session))) == 0

        # test status
        with session_scope() as session:
            notification.status = notification_pb2.Notification.Status.PENDING
            session.merge(to_model(notification))

            assert len(list(sender.select_notifications(session))) == 1

    # def test_select_events(self):
    #     event_1 = event_pb2.Event(id=str(uuid.uuid4()))
    #     event_2 = event_pb2.Event(id=str(uuid.uuid4()), process=process_pb2.Process(id=str(uuid.uuid4())))
    #     event_3 = event_pb2.Event(
    #         id=str(uuid.uuid4()), process=process_pb2.Process(id=str(uuid.uuid4()), subprocesses=[process_pb2.SubProcess(id=str(uuid.uuid4()))])
    #     )
    #     with session_scope() as session:
    #         session.merge(to_model(event_1))
    #         session.merge(to_model(event_2))
    #         session.merge(to_model(event_3))
    #
    #         # test empty select
    #         assert len(list(NotifySender.select_events(session, [], [], []))) == 0
    #
    #         # test events select
    #         ns = NotifySender.select_events(session, [str(uuid.uuid4()), event_1.id], [], [])
    #         assert len(ns) == 1
    #         assert str(ns[0].id) == event_1.id
    #
    #         # test processes select
    #         ns = NotifySender.select_events(session, [], [str(uuid.uuid4()), event_2.process.id], [])
    #         assert len(ns) == 1
    #         assert str(ns[0].id) == event_2.id
    #
    #         # test subprocesses select
    #         ns = NotifySender.select_events(session, [], [], [str(uuid.uuid4()), event_3.process.subprocesses[0].id])
    #         assert len(ns) == 1
    #         assert str(ns[0].id) == event_3.id
    #
    #         # test composite select
    #         ns = NotifySender.select_events(
    #             session,
    #             [str(uuid.uuid4()), event_1.id],
    #             [str(uuid.uuid4()), event_2.process.id],
    #             [str(uuid.uuid4()), event_3.process.subprocesses[0].id],
    #         )
    #         assert len(ns) == 3
    #         assert sorted(str(n.id) for n in ns) == sorted((event_1.id, event_2.id, event_3.id))

    def test_run_once(self):
        sender = NotifySender()

        notification = notification_pb2.Notification(
            id=str(uuid.uuid4()),
            type=notification_pb2.Notification.Type.EXECUTION_STARTS,
            event_id=str(uuid.uuid4()),
            message='test',
        )
        event = event_pb2.Event(
            id=notification.event_id,
            config=event_pb2.EventConfig(
                flow='test',
            ),
            component=component_pb2.Component(
                id=notification.event_id,
                flows=component_pb2.FlowList(
                    objects=[
                        component_pb2.Flow(
                            id='test',
                        ),
                    ],
                ),
            ),
        )

        # test invalid notification - event
        with session_scope() as session:
            session.merge(to_model(notification))

            sender.run_once()
            assert session.query(model.Notification).first().status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.FAILED]

        # test invalid notification - component
        with session_scope() as session:
            session.merge(to_model(notification))
            session.merge(model.Event(id=notification.event_id))

            sender.run_once()
            assert session.query(model.Notification).first().status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.FAILED]

        # test invalid notification - flow
        with session_scope() as session:
            session.merge(to_model(event_pb2.Event(id=event.id, config=event.config, component=component_pb2.Component(id=event.id))))
            session.merge(to_model(notification))

            sender.run_once()
            assert session.query(model.Notification).first().status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.FAILED]

        # test valid notification
        with session_scope() as session:
            session.merge(to_model(event))
            session.merge(to_model(notification))

            sender.run_once()
            assert session.query(model.Notification).first().status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.DELIVERED]
            assert len(self.clients.bot.notifications) == 1

    def test_run_once_bot(self):
        sender = NotifySender()

        notification = notification_pb2.Notification(
            id=str(uuid.uuid4()),
            type=notification_pb2.Notification.Type.EXECUTION_STARTS,
            event_id=str(uuid.uuid4()),
            message='test',
        )
        event = event_pb2.Event(
            id=notification.event_id,
            config=event_pb2.EventConfig(
                flow='test',
            ),
            component=component_pb2.Component(
                id=notification.event_id,
                flows=component_pb2.FlowList(
                    objects=[
                        component_pb2.Flow(
                            id='test',
                            nanny=component_pb2.NannyFlow(
                                dashboard='test',
                                recipe='test',
                            )
                        ),
                    ],
                ),
            ),
        )

        # test valid bot notification - start(end, error, message)
        with session_scope() as session:
            session.merge(to_model(notification))
            session.merge(to_model(event))

            sender.run_once()
            assert session.query(model.Notification).first().status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.DELIVERED]
            assert len(self.clients.bot.notifications) == 1

            assert self.clients.bot.notifications[-1]['recipe'] == event.component.flows.objects[-1].nanny.recipe
            assert self.clients.bot.notifications[-1]['dashboard'] == event.component.flows.objects[-1].nanny.dashboard
            assert self.clients.bot.notifications[-1]['status'] == sender.BOT_STATUS[notification.type]
            assert self.clients.bot.notifications[-1]['message'] == f'{event.description.title}\n{notification.message}'
            assert self.clients.bot.notifications[-1]['event_id'] == event.id

        # test valid bot notification - confirm
        with session_scope() as session:
            notification.type = notification_pb2.Notification.Type.CONFIRM
            session.merge(to_model(notification))

            sender.run_once()
            assert session.query(model.Notification).first().status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.DELIVERED]
            assert len(self.clients.bot.notifications) == 2

            assert self.clients.bot.notifications[-1]['should_confirm'] is True

        self.clients.bot.unload()

    def test_run_once_bot_new(self):
        sender = NotifySender()

        notification = notification_pb2.Notification(
            id=str(uuid.uuid4()),
            type=notification_pb2.Notification.Type.EXECUTION_STARTS,
            event_id=str(uuid.uuid4()),
            message='test',
            config=common_pb2.NotificationConfig(notify_marty=True)
        )
        recipe_deploy_id = 'recipe_deploy_id'
        event = event_pb2.Event(
            id=notification.event_id,
            config=event_pb2.EventConfig(
                flow='test',
            ),
            component=component_pb2.Component(
                id=notification.event_id,
                flows=component_pb2.FlowList(
                    objects=[
                        component_pb2.Flow(
                            id='test',
                            nanny=component_pb2.NannyFlow(
                                dashboard='test',
                                recipe='test',
                            )
                        ),
                    ],
                ),
                notifications=common_pb2.NotificationConfig(
                    message_template="any message"
                )
            ),
            process=process_pb2.Process(
                subprocesses=[
                    process_pb2.SubProcess(
                        tasks=[
                            executor_pb2.ExecutionTask(
                                state=executor_pb2.ExecutionTaskState(
                                    deploy_nanny_recipe=executor_pb2.DeployNannyRecipeTaskState(
                                        recipe_deploy_id=recipe_deploy_id
                                    )
                                )
                            )
                        ]
                    )
                ]
            )
        )

        marty = 'lymarenkolev'
        self.clients.shift.set_marty(marty)

        with session_scope() as session:
            session.merge(to_model(notification))
            session.merge(to_model(event))

            sender.run_once()

            notification_entry = (
                session.query(model.Notification)
                .filter(model.Notification.id == notification.id)
                .one()
            )

            assert notification_entry.status == notification_pb2.Notification.Status[notification_pb2.Notification.Status.DELIVERED], notification_entry.error
            assert len(self.clients.bot.notification_contexts) == 1

            context = self.clients.bot.notification_contexts[0]

            assert context.recipe == event.component.flows.objects[-1].nanny.recipe
            assert context.event_id == event.id
            assert context.dashboard == event.component.flows.objects[-1].nanny.dashboard
            assert context.status == sender.BOT_STATUS[notification.type]
            assert context.message == f'{event.description.title}\n{notification.message}'
            assert context.marty == marty
            assert context.deploy_id == recipe_deploy_id

        self.clients.bot.unload()
