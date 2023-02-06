# coding: utf-8

import os
import tempfile
from unittest import TestCase

from market.pylibrary.common.env import Env, skip_if, SkipFunction
from market.pylibrary.common.context import updated_mapping


class TestEnv(TestCase):

    def test_assert(self):
        update = {'ENV_TYPE': 'pr', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            with self.assertRaises(AssertionError):
                Env()

        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'asdf'}
        with updated_mapping(os.environ, update):
            with self.assertRaises(AssertionError):
                Env()

    def test_env_vars(self):
        update = {'ENV_TYPE': 'prestable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            env = Env()
            self.assertEqual(env.envtype, 'prestable')
            self.assertEqual(env.mitype, 'stratocaster')

        update = {'BSCONFIG_ITAGS': 'test_val a_ctype_stable other_val', 'MI_TYPE': 'gibson'}
        with updated_mapping(os.environ, update, delete=['ENV_TYPE']):
            env = Env()
            self.assertEqual(env.envtype, 'stable')
            self.assertEqual(env.mitype, 'gibson')

    def test_by_path(self):
        delete = ('ENV_TYPE', 'MI_TYPE', 'BSCONFIG_ITAGS')
        with updated_mapping(os.environ, delete=delete):
            with tempfile.NamedTemporaryFile(mode='w+') as envtype_fobj:
                with tempfile.NamedTemporaryFile(mode='w+') as mitype_fobj:
                    envtype_fobj.write('  development\n')
                    mitype_fobj.write('planeshift.gibson\n')
                    envtype_fobj.flush()
                    mitype_fobj.flush()

                    env = Env(envtype_path=envtype_fobj.name, mitype_path=mitype_fobj.name)
                    self.assertEqual(env.envtype, 'development')
                    self.assertEqual(env.mitype, 'planeshift.gibson')

    def test_getattr(self):
        update = {'ENV_TYPE': 'prestable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            env = Env()

            self.assertTrue(env.is_prestable)
            for envtype_name in ('stable', 'production', 'dev', 'testing'):
                self.assertFalse(getattr(env, 'is_' + envtype_name))

            self.assertTrue(env.is_strat)
            for envtype_name in ('gibson', 'planeshift', 'planeshift_gibson'):
                self.assertFalse(getattr(env, 'is_' + envtype_name))


class TestSkipIf(TestCase):

    def _func(self, test_kwarg=None):
        pass

    def test_error(self):
        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            skip_if(envtype='prestable', with_error=False)(self._func)()
            skip_if(envtype='prestable', with_error=True)(self._func)()

            with self.assertRaises(SkipFunction):
                skip_if(envtype='stable', with_error=True)(self._func)()
            skip_if(envtype='stable', with_error=False)(self._func)()

    def test_envtype(self):
        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            skip_if(envtype='prestable')(self._func)()
            skip_if(envtype=('prestable', 'testing'))(self._func)()

            with self.assertRaises(SkipFunction):
                skip_if(envtype='stable')(self._func)()
            with self.assertRaises(SkipFunction):
                skip_if(envtype='prod')(self._func)()
            with self.assertRaises(SkipFunction):
                skip_if(envtype=('dev', 'prod'))(self._func)()

    def test_mitype(self):
        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            skip_if(mitype='gibson')(self._func)()
            skip_if(mitype=('gibson', 'planeshift'))(self._func)()

            with self.assertRaises(SkipFunction):
                skip_if(mitype='strat')(self._func)()
            with self.assertRaises(SkipFunction):
                skip_if(mitype='stratocaster')(self._func)()
            with self.assertRaises(SkipFunction):
                skip_if(mitype=('gibson', 'stratocaster'))(self._func)()

    def test_envtype_mitype(self):
        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            skip_if(envtype='prestable', mitype='gibson')(self._func)()
            skip_if(envtype='stable', mitype='gibson')(self._func)()
            skip_if(envtype='testing', mitype='strat')(self._func)()

            with self.assertRaises(SkipFunction):
                skip_if(envtype='stable', mitype='strat')(self._func)()

            skip_if(envtype=('prestable', 'prod'), mitype='gibson')(self._func)()

            with self.assertRaises(SkipFunction):
                skip_if(envtype=('prestable', 'prod'), mitype=('gibson', 'strat'))(self._func)()

    def test_check_kwargs(self):
        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster'}
        with updated_mapping(os.environ, update):
            dec = skip_if(check_kwargs=lambda test_kwarg: test_kwarg)
            dec(self._func)(test_kwarg=False)

            with self.assertRaises(SkipFunction):
                dec(self._func)(test_kwarg=True)

            dec = skip_if(
                envtype='prestable',
                check_kwargs=lambda test_kwarg: test_kwarg,
            )
            dec(self._func)(test_kwarg=True)

            dec = skip_if(
                envtype='prod',
                check_kwargs=lambda test_kwarg: test_kwarg,
            )
            with self.assertRaises(SkipFunction):
                dec(self._func)(test_kwarg=True)

    def test_env_var(self):
        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster', 'DISABLE_SKIP_IF': '1'}
        with updated_mapping(os.environ, update):
            skip_if(envtype='stable')(self._func)()

        update = {'ENV_TYPE': 'stable', 'MI_TYPE': 'stratocaster', 'DISABLE_SKIP_IF': ''}
        with updated_mapping(os.environ, update):
            with self.assertRaises(SkipFunction):
                skip_if(envtype=('dev', 'prod'))(self._func)()
