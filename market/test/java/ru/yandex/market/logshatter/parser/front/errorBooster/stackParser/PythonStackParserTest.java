package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PythonStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "LockNotAvailable: canceling statement due to lock timeout\n" +
                "\n" +
                "  File \"sqlalchemy/engine/base.py\", line 1244, in _execute_context\n" +
                "    cursor, statement, parameters, context\n" +
                "  File \"sqlalchemy/engine/default.py\", line 550, in do_execute\n" +
                "    cursor.execute(statement, parameters)\n" +
                "OperationalError: (psycopg2.errors.LockNotAvailable) canceling statement due to lock timeout\n" +
                "\n" +
                "[SQL: REFRESH MATERIALIZED VIEW CONCURRENTLY v_vacancies_for_board_9;]\n" +
                "(Background on this error at: http://sqlalche.me/e/e3q8)\n" +
                "  File \"celery/app/trace.py\", line 385, in trace_task\n" +
                "    R = retval = fun(*args, **kwargs)\n" +
                "  File \"celery/app/trace.py\", line 648, in __protected_call__\n" +
                "    return self.run(*args, **kwargs)\n" +
                "  File \"celery/app/base.py\", line 472, in run\n" +
                "    return task._orig_run(*args, **kwargs)\n" +
                "  File \"analytics_services/tasks/schedule.py\", line 53, in refresh_view_vacancies_for_board\n" +
                "    ViewVacanciesForBoard.refresh(db_session=db_session)\n" +
                "  File \"sqlalchemy/engine/default.py\", line 550, in do_execute\n" +
                "    cursor.execute(statement, parameters)",
            new StackFrame("trace_task", "celery/app/trace.py", 385, 0),
            new StackFrame("__protected_call__", "celery/app/trace.py", 648, 0),
            new StackFrame("run", "celery/app/base.py", 472, 0),
            new StackFrame("refresh_view_vacancies_for_board", "analytics_services/tasks/schedule.py", 53, 0),
            new StackFrame("do_execute", "sqlalchemy/engine/default.py", 550, 0)
        ),

        new TestData(
            "Exception: ('No address for candidate %s', '56faca15-a34f-48b2-8032-1b66c850c4d4.talents')\n" +
                "  File \"celery/app/trace.py\", line 385, in trace_task\n" +
                "    R = retval = fun(*args, **kwargs)\n" +
                "  File \"analytics_services/tasks/candidate_base.py\", line 25, in update_or_create_candidate\n" +
                "    update_candidate(user_id, db_session=db_session)\n" +
                "  File \"analytics_services/core/controllers/candidates_for_share.py\", line 54, in set_base_info\n" +
                "    raise Exception('No address for candidate %s', candidate_data.user_id)",
            new StackFrame("trace_task", "celery/app/trace.py", 385, 0),
            new StackFrame("update_or_create_candidate", "analytics_services/tasks/candidate_base.py", 25, 0),
            new StackFrame("set_base_info", "analytics_services/core/controllers/candidates_for_share.py", 54, 0)
        ),

        new TestData(
            "MaybeEncodingError: Error sending result: ''(1, <ExceptionInfo: StateLockAcquiringError()>, None)''. " +
                "Reason: ''PicklingError(\"Can\\'t pickle <class \\'talents_core.sm.errors" +
                ".StateLockAcquiringError\\'>: it\\'s not the same object as talents_core.sm.errors" +
                ".StateLockAcquiringError\")''.\n" +
                "  File \"billiard/pool.py\", line 362, in workloop\n" +
                "    put((READY, (job, i, result, inqW_fd)))\n" +
                "  File \"billiard/queues.py\", line 366, in put\n" +
                "    self.send_payload(ForkingPickler.dumps(obj))\n" +
                "  File \"billiard/reduction.py\", line 56, in dumps\n" +
                "    cls(buf, protocol).dump(obj)",
            new StackFrame("workloop", "billiard/pool.py", 362, 0),
            new StackFrame("put", "billiard/queues.py", 366, 0),
            new StackFrame("dumps", "billiard/reduction.py", 56, 0)
        ),
        new TestData(
            "NoSuchKey: An error occurred (NoSuchKey) when calling the GetObject operation: The specified key does " +
                "not exist.\n" +
                "  File \"raven/middleware.py\", line 100, in __call__\n" +
                "    iterable = self.application(environ, start_response)\n" +
                "  File \"admin_backend/admin/resources/applications_clients_files.py\", line 71, in on_get\n" +
                "    self.applications_file_id.on_get(req, resp, filename, FinalResumeFileTypes.many_clients_excel)\n" +
                "  File \"botocore/client.py\", line 661, in _make_api_call\n" +
                "    raise error_class(parsed_response, operation_name)",
            new StackFrame("__call__", "raven/middleware.py", 100, 0),
            new StackFrame("on_get", "admin_backend/admin/resources/applications_clients_files.py", 71, 0),
            new StackFrame("_make_api_call", "botocore/client.py", 661, 0)
        ),

        new TestData(
            "StateMachineError: null\n" +
                "  File \"admin_backend/admin/resources/organizaion_moderation.py\", line 43, in on_post\n" +
                "    state=state\n" +
                "  File \"admin_backend/db/controllers/sprav_moderation.py\", line 250, in organization_transition\n" +
                "    session=session,\n" +
                "  File \"talents_core/sm/base_sm.py\", line 64, in transition\n" +
                "    trans(obj=obj, context=context, state=state, session=session, event=event)\n" +
                "  File \"admin_backend/sm/sprav_moderation_trans.py\", line 40, in apply_moderation\n" +
                "    _check_yang_task_is_ready(obj)\n" +
                "  File \"admin_backend/sm/sprav_moderation_trans.py\", line 184, in _check_yang_task_is_ready\n" +
                "    params={'yang_task': obj.yang_task},",
            new StackFrame("on_post", "admin_backend/admin/resources/organizaion_moderation.py", 43, 0),
            new StackFrame("organization_transition", "admin_backend/db/controllers/sprav_moderation.py", 250, 0),
            new StackFrame("transition", "talents_core/sm/base_sm.py", 64, 0),
            new StackFrame("apply_moderation", "admin_backend/sm/sprav_moderation_trans.py", 40, 0),
            new StackFrame("_check_yang_task_is_ready", "admin_backend/sm/sprav_moderation_trans.py", 184, 0)
        ),

        new TestData(
            "HTTPError: 400 Client Error: Bad Request for url: https://analytics-services.prod.talents.yandex" +
                ".net/dialogger_result\n" +
                "  File \"admin_backend/user_profile/controller.py\", line 355, in update_search_params\n" +
                "    ClientsDI.analytics_services_client().send_competences(competences)\n" +
                "  File \"admin_backend/utils/clients/analytics_services.py\", line 25, in send_competences\n" +
                "    response = self._post(self.send_competences_url, data=competences)\n" +
                "  File \"talents_common/retry.py\", line 30, in decorated\n" +
                "    return func(*args, **kwargs)\n" +
                "  File \"admin_backend/utils/clients/analytics_services.py\", line 95, in _post\n" +
                "    response.raise_for_status()\n" +
                "  File \"requests/models.py\", line 940, in raise_for_status\n" +
                "    raise HTTPError(http_error_msg, response=self)",
            new StackFrame("update_search_params", "admin_backend/user_profile/controller.py", 355, 0),
            new StackFrame("send_competences", "admin_backend/utils/clients/analytics_services.py", 25, 0),
            new StackFrame("decorated", "talents_common/retry.py", 30, 0),
            new StackFrame("_post", "admin_backend/utils/clients/analytics_services.py", 95, 0),
            new StackFrame("raise_for_status", "requests/models.py", 940, 0)
        ),

        new TestData(
            "timeout: timed out\n" +
                "  File \"urllib3/connection.py\", line 160, in _new_conn\n" +
                "    (self._dns_host, self.port), self.timeout, **extra_kw)\n" +
                "  File \"urllib3/util/connection.py\", line 80, in create_connection\n" +
                "    raise err\n" +
                "  File \"urllib3/util/connection.py\", line 70, in create_connection\n" +
                "    sock.connect(sa)\n" +
                "ConnectTimeoutError: (<urllib3.connection.HTTPConnection object at 0x7fa7a22a2e50>, 'Connection to " +
                "geobase.qloud.yandex.ru timed out. (connect timeout=0.25)')\n" +
                "  File \"urllib3/connectionpool.py\", line 603, in urlopen\n" +
                "    chunked=chunked)\n" +
                "  File \"urllib3/connectionpool.py\", line 355, in _make_request\n" +
                "    conn.request(method, url, **httplib_request_kw)\n" +
                "  File \"http/client.py\", line 1252, in request\n" +
                "    self._send_request(method, url, body, headers, encode_chunked)\n" +
                "  File \"http/client.py\", line 1298, in _send_request\n" +
                "    self.endheaders(body, encode_chunked=encode_chunked)\n" +
                "  File \"http/client.py\", line 1247, in endheaders\n" +
                "    self._send_output(message_body, encode_chunked=encode_chunked)\n" +
                "  File \"http/client.py\", line 1026, in _send_output\n" +
                "    self.send(msg)\n" +
                "  File \"http/client.py\", line 966, in send\n" +
                "    self.connect()\n" +
                "  File \"urllib3/connection.py\", line 183, in connect\n" +
                "    conn = self._new_conn()\n" +
                "  File \"urllib3/connection.py\", line 165, in _new_conn\n" +
                "    (self.host, self.timeout))\n" +
                "MaxRetryError: HTTPConnectionPool(host='geobase.qloud.yandex.ru', port=80): Max retries exceeded " +
                "with url: /v1/parents?id=45 (Caused by ConnectTimeoutError(<urllib3.connection.HTTPConnection object" +
                " at 0x7fa7a22a2e50>, 'Connection to geobase.qloud.yandex.ru timed out. (connect timeout=0.25)'))\n" +
                "  File \"requests/adapters.py\", line 449, in send\n" +
                "    timeout=timeout\n" +
                "  File \"urllib3/connectionpool.py\", line 641, in urlopen\n" +
                "    _stacktrace=sys.exc_info()[2])\n" +
                "  File \"urllib3/util/retry.py\", line 399, in increment\n" +
                "    raise MaxRetryError(_pool, url, error or ResponseError(cause))\n" +
                "ConnectTimeout: HTTPConnectionPool(host='geobase.qloud.yandex.ru', port=80): Max retries exceeded " +
                "with url: /v1/parents?id=45 (Caused by ConnectTimeoutError(<urllib3.connection.HTTPConnection object" +
                " at 0x7fa7a22a2e50>, 'Connection to geobase.qloud.yandex.ru timed out. (connect timeout=0.25)'))\n" +
                "  File \"admin_backend/board/controllers/geo_info.py\", line 80, in area_center_city_geo_id\n" +
                "    parent_geo_ids = ClientsDI.geo_base_client().get_parents_geo_id(geo_id)\n" +
                "  File \"admin_backend/utils/clients/geo.py\", line 194, in get_parents_geo_id\n" +
                "    response = self._get_geobase_resp(method='parents', params={'id': geo_id})\n" +
                "  File \"admin_backend/utils/clients/geo.py\", line 221, in _get_geobase_resp\n" +
                "    resp = self.session.get(_url, params=params or {}, timeout=0.25)  # 250ms\n" +
                "  File \"requests/adapters.py\", line 504, in send\n" +
                "    raise ConnectTimeout(e, request=request)",
            new StackFrame("area_center_city_geo_id", "admin_backend/board/controllers/geo_info.py", 80, 0),
            new StackFrame("get_parents_geo_id", "admin_backend/utils/clients/geo.py", 194, 0),
            new StackFrame("_get_geobase_resp", "admin_backend/utils/clients/geo.py", 221, 0),
            new StackFrame("send", "requests/adapters.py", 504, 0)
        ),

        new TestData(
            "LockNotAcquiredError: Could not acquire lock for resource \"generate_feed_vacancies_lock\"\n" +
                "  File \"admin_backend/feed/vacancies/tasks.py\", line 22, in generate_vacancies_feed\n" +
                "    generate_feed_vacancies_action(format_=format_)\n" +
                "  File \"talents_core/db/analytics/lock.py\", line 332, in _wrapper\n" +
                "    session=session, timeout=datetime.timedelta(seconds=1)  # acquiring lock timeout\n" +
                "  File \"talents_core/db/analytics/lock.py\", line 87, in lock\n" +
                "    raise LockNotAcquiredError(self._resource)",
            new StackFrame("generate_vacancies_feed", "admin_backend/feed/vacancies/tasks.py", 22, 0),
            new StackFrame("_wrapper", "talents_core/db/analytics/lock.py", 332, 0),
            new StackFrame("lock", "talents_core/db/analytics/lock.py", 87, 0)
        ),

        // Empty
        new TestData(
            ""
        ),
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    @Test
    public void parseBigStack() throws IOException {
        // https://st.yandex-team.ru/MARKETINFRA-7544
        String stack = FileUtils.readFileToString(new File(
            Objects.requireNonNull(getClass().getClassLoader().getResource(
                "ru/yandex/market/logshatter/parser/front/errorBooster/stackParser/pythonBigStack.txt"
            )).getPath()
        ));

        StackFrame[] actualFrames = new PythonStackParser(stack).getStackFrames();

        assertEquals(991, actualFrames.length);
    }

    static class TestData {
        private final String stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = stack;
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new PythonStackParser(stack).getStackFrames();
            assertArrayEquals(
                frames,
                actualFrames,
                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message"
            );
        }
    }
}
