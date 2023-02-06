#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.library.shiny.server.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty


class T(env.TestSuite):
    def test_ping(self):
        response = self.shiny.request_text('ping')
        self.assertFragmentIn(response, '0;OK')

    def test_info(self):
        response = self.shiny.request_json('info')
        self.assertFragmentIn(response, {
            'revision': NotEmpty(),
            'date': NotEmpty(),
            'name': 'shiny'
        })

    def test_reopen_log(self):
        response = self.shiny.request_json('reopen_log')
        self.assertFragmentIn(response, {'status': 'ok'})

    def test_open_close(self):
        self.assertFragmentIn(self.shiny.request_text('ping'), '0;OK')
        self.assertFragmentIn(self.shiny.request_text('close'), '0;OK')
        self.assertFragmentIn(self.shiny.request_text('ping', fail_on_error=False), '2;CLOSED')
        self.common_log.expect(
            message='Error response: 2;CLOSED, code: 500',
            severity='ERRR'
        )

        self.assertFragmentIn(self.shiny.request_text('open'), '0;OK')
        self.assertFragmentIn(self.shiny.request_text('ping'), '0;OK')

    def test_help(self):
        self.assertFragmentIn(
            self.shiny.request_json('help'),
            {
                '/close GET': 'Close service for load',
                '/help GET': 'Brief description about all handles',
                '/info GET': 'General information about service',
                '/monitoring GET': 'Service health status',
                '/open GET': 'Open service for load',
                '/ping GET': 'Service live check',
                '/reopen_log GET': 'Reopen service log on rotate',
                '/stat GET': 'Provides statistics to YASM',
                '/test/monitoring GET': 'Test monitoring switch handler'
            }
        )

    def test_help_inference(self):
        response = self.shiny.request_json('test/post_monitoring?help', fail_on_error=False)
        self.assertEqual(200, response.code)

    def test_monitoring(self):
        self.assertFragmentIn(self.shiny.request_text('monitoring'), '0;OK')
        self.shiny.request_json('test/monitoring?error=crash')
        self.assertFragmentIn(
            self.shiny.request_text('monitoring', fail_on_error=False),
            '2;Test health check(crash)'
        )

        self.common_log.expect(
            message="Error response: 2;Test health check(crash), code: 500",
            severity='ERRR'
        )

        self.assertFragmentIn(self.shiny.request_text('ping'), '0;OK')
        self.shiny.request_json('test/monitoring?clear')
        self.assertFragmentIn(self.shiny.request_text('monitoring'), '0;OK')

    def test_stat(self):
        self.shiny.request_text('ping')
        self.shiny.request_text('close')
        self.shiny.request_text('ping', fail_on_error=False)
        self.common_log.expect(message="Error response: 2;CLOSED, code: 500", severity='ERRR')
        self.shiny.request_text('open')
        rev = self.shiny.request_json('info')['revision']

        response = self.shiny.request_json('stat')
        self.assertFragmentIn(response, [
            ['shiny_component=server;request_count_dmmm', NotEmpty()],
            ['shiny_component=server;request_time_hgram', NotEmpty()],
            ['shiny_component=server;ping_GET_request_count_dmmm', NotEmpty()],
            ['shiny_component=server;ping_GET_request_time_hgram', NotEmpty()],
            ['shiny_component=server;stat_GET_request_count_dmmm', NotEmpty()],
            ['shiny_component=server;server_opened_dmmm', NotEmpty()],
            ['shiny_component=server;server_closed_dmmm', NotEmpty()],
            ['shiny_component=server;server_revision_{}_dmmm'.format(rev), NotEmpty()],
            ['is_alive_ammv', 1]
        ])

        response = self.shiny.request_json('stat?reset')
        self.assertFragmentIn(response, [
            ['shiny_component=server;request_count_dmmm', 0],
            ['shiny_component=server;ping_GET_request_count_dmmm', 0],
            ['shiny_component=server;stat_GET_request_count_dmmm', 0],
            ['is_alive_ammv', 1]
        ])

    def test_position_arguments(self):
        """ Позиционные аргументы задаются через шаблон '<>' """
        self.assertFragmentIn(
            self.shiny.request_json('test/position/123/some_node/another_id'),
            {
                'SomeId': 123,
                'AnotherId': 'another_id',
                'OverrideName': 'Default',
            }
        )

    def test_position_arguments_absorption(self):
        """ Если не удается найти подходящего хендлера по точному соответствию, выбирается хендлер с наиболее длинным префиксом и обработкой wildcard'a """
        self.assertFragmentIn(
            self.shiny.request_text('test/position/123/some_node/another_id/some_other_leaf'),
            '123 /some_node/another_id/some_other_leaf?format=text'
        )

    def test_position_arguments_override(self):
        """ Запросы с позиционными аргументами можно уточнять """
        self.assertFragmentIn(
            self.shiny.request_json('test/position/123/some_node/another_id/some_leaf'),
            {
                'SomeId': 123,
                'AnotherId': 'another_id',
                'OverrideName': 'Specific',
            }
        )

    def test_wildcard_arguments(self):
        """ wildcard-аргументы работают вместе с позиционными """
        self.assertFragmentIn(
            self.shiny.request_text('test/position/123/some_leaf?say=goodbye'),
            '123 /some_leaf?say=goodbye&format=text',
        )

        self.assertFragmentIn(
            self.shiny.request_text('test/position/123/some_wildcard_node/some_leaf?say=goodbye'),
            '123 /some_wildcard_node/some_leaf?say=goodbye&format=text',
        )

        self.assertFragmentIn(
            self.shiny.request_text('test/wildcard/123/wildcard_node/some_leaf?say=goodbye'),
            '123 /some_leaf?say=goodbye&format=text',
        )

    def test_wildcard_root(self):
        self.assertFragmentIn(
            self.shiny.request_json('unknown_handler'),
            {'handler': '/unknown_handler?format=json'}
        )

        self.assertFragmentIn(
            self.shiny.request_json(''),
            {'handler': '?format=json'}
        )


if __name__ == '__main__':
    env.main()
