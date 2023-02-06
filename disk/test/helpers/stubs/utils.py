# -*- coding: utf-8 -*-
import os
import traceback

import mock

import mpfs
import mpfs.engine.process
from mpfs.common.util import iterdbuids
from test.fixtures.users import default_user, usr_1, usr_2, usr_3
from test.helpers.stubs import base


class IterdbuidsStub(base.ChainedPatchBaseStub):
    """
    Заглушка для iterdbuids.run
    """

    def __init__(self):
        super(IterdbuidsStub, self).__init__()
        self.run = mock.patch('mpfs.common.util.iterdbuids.run', new=self._run)
        self.get_uids_from_db = mock.patch('mpfs.common.util.iterdbuids.get_uids', new=self._get_uids)

    @staticmethod
    def _run(process, callback, file_name, **kwargs):
        log = kwargs.pop('log', None) or mpfs.engine.process.get_default_log()

        uids = None

        try:
            for uid in iterdbuids.get_uids(file_name, log, **kwargs):
                process(uid)

            # удаляем файл после успешной обработки всех uid'ов
            os.remove(file_name)
            log.info('File "%s" fully processed, removed' % file_name)
        except Exception, e:
            log.error('Error occurred while executing %s: %s' % (process, e))
            log.error(traceback.format_exc())
            log.error('Terminating.')
        finally:
            if uids is not None:
                uids.close()

    @staticmethod
    def _get_uids(file_name, log, **kwargs):
        for user in (default_user, usr_1, usr_2, usr_3):
            uid = user.get('uid') or user.get('_id')
            yield uid
