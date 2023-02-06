# -*- coding: utf-8 -*-
from datetime import datetime, time, date

from freezegun import freeze_time

from travel.rasp.library.python.common23.date import environment
from travel.rasp.suburban_tasks.suburban_tasks.models import Full_STRAINS, Current_STRAINS, Update, Current_STRAINSVAR, Full_STRAINSVAR, Full_SRASPRP, \
    Current_SRASPRP, Full_SCALENDAR, Current_SCALENDAR, IC00_STAN, Full_SDOCS, Current_SDOCS, Change_SPEC_BUF, \
    Change_STRAINSVAR_BUF, Change_SRASPRP_BUF, Change_SCALENDAR_BUF, Change_SDOCS_BUF, Change_STRAINS_BUF
from common.tester.factories import ModelFactory, factories


DEFAULT_INTEGER = -65555
DEFAULT_YEAR = 2000
DETAULT_LETTERS = u'ZZZ'
DETAULT_TRAIN_NAME = u'TRAIN_NAME_ARROW'
DEFAULT_NDAYS = u'EVERY DAY'
DEFAULT_FLOAT = -100.100
DEFAULT_DATE = date(2000, 1, 1)
DEFAULT_DATETIME = datetime(2000, 1, 1, 15)


class RawDataMixing(object):
    @classmethod
    def make_rzd_db_row(cls, **kwargs):
        row = cls.rzd_kwargs.copy()
        row.update(kwargs)
        return row


def build_datetime(param):
    if isinstance(param, basestring):
        parts = param.split()
        datetime_params = map(int, parts[0].split('-'))
        if len(parts) == 2:
            datetime_params += map(int, parts[1].split(':'))

        return datetime(*datetime_params)

    else:
        return param


def build_date(param):
    if isinstance(param, basestring):
        datetime_params = map(int, param.split('-'))

        return datetime(*datetime_params)

    else:
        return param


def register_factory(ModelFactoryClass):
    factories[ModelFactoryClass.Model] = ModelFactoryClass()
    return factories[ModelFactoryClass.Model]


class UpdateFactory(ModelFactory):
    default_kwargs = {
        'updated_at': environment.now,
        'last_gvc_date': environment.now
    }
    Model = Update

    def create_object(self, kwargs):
        assert kwargs['action_type'] in (Update.FULL_UPDATE, Update.CHANGES_UPDATE)
        kwargs['updated_at'] = build_datetime(kwargs.pop('updated_at', None))
        kwargs['last_gvc_date'] = build_datetime(kwargs.pop('last_gvc_date', None))
        kwargs['query_from_dt'] = build_datetime(kwargs.pop('query_from_dt', None))
        kwargs['query_to_dt'] = build_datetime(kwargs.pop('query_to_dt', None))

        with freeze_time(kwargs['updated_at']):
            return super(UpdateFactory, self).create_object(kwargs)


create_update = register_factory(UpdateFactory)


class STRAINS_Factory(ModelFactory, RawDataMixing):
    rzd_kwargs = default_kwargs = {
        'IDTR': DEFAULT_INTEGER,
        'YEAR': DEFAULT_YEAR,
        'CREG': DEFAULT_INTEGER,
        'NUM': DEFAULT_INTEGER,
        'LETTERS': DETAULT_LETTERS,
        'RP1': DEFAULT_INTEGER,
        'RPK': DEFAULT_INTEGER,
        'NAME': DETAULT_TRAIN_NAME,
        'CATEGORY': DEFAULT_INTEGER,
        'IDROOT': DEFAULT_INTEGER,
        'IDSOOB': DEFAULT_INTEGER,
        'CODEOWNER': DEFAULT_INTEGER,
        'NDAYS': DEFAULT_NDAYS,
        'STATE': DEFAULT_INTEGER,
        'IDR': DEFAULT_INTEGER,
    }


class Full_STRAINS_Factory(STRAINS_Factory):
    Model = Full_STRAINS

create_full_strains = register_factory(Full_STRAINS_Factory)


class Current_STRAINS_Factory(STRAINS_Factory):
    Model = Current_STRAINS

create_current_strains = register_factory(Current_STRAINS_Factory)


class Change_STRAINS_BUF_Factory(STRAINS_Factory):
    rzd_kwargs = STRAINS_Factory.rzd_kwargs.copy()
    rzd_kwargs.update({
        'DATE_GVC': DEFAULT_DATE,
        'ID_XML': DEFAULT_INTEGER,
        'SIST_IST': DEFAULT_INTEGER
    })
    default_kwargs = STRAINS_Factory.default_kwargs.copy()
    default_kwargs.update(rzd_kwargs)
    Model = Change_STRAINS_BUF

STRAINS_BUF_Factory = Change_STRAINS_BUF_Factory
create_strains_buf = register_factory(STRAINS_BUF_Factory)


class STRAINSVAR_Factory(ModelFactory, RawDataMixing):
    rzd_kwargs = default_kwargs = {
        'IDTR': DEFAULT_INTEGER,
        'IDR': DEFAULT_INTEGER,
        'YEAR': DEFAULT_YEAR,
        'RP1': DEFAULT_INTEGER,
        'RPK': DEFAULT_INTEGER,
        'VPUTI': DEFAULT_INTEGER,
        'KM': DEFAULT_FLOAT,
        'STOPS': DEFAULT_INTEGER,
        'STATE': DEFAULT_INTEGER,
        'CREG': DEFAULT_INTEGER,
        'RASPTYPE': DEFAULT_INTEGER,
        'BASERASPID': DEFAULT_INTEGER,
        'IDDOC': DEFAULT_INTEGER,
    }


class Full_STRAINSVAR_Factory(STRAINSVAR_Factory):
    Model = Full_STRAINSVAR

create_full_strainsvar = register_factory(Full_STRAINSVAR_Factory)


class Current_STRAINSVAR_Factory(STRAINSVAR_Factory):
    Model = Current_STRAINSVAR

create_current_strainsvar = register_factory(Current_STRAINSVAR_Factory)


class Change_STRAINSVAR_BUF_Factory(STRAINSVAR_Factory):
    rzd_kwargs = STRAINSVAR_Factory.rzd_kwargs.copy()
    rzd_kwargs.update({
        'DATE_GVC': DEFAULT_DATE,
        'ID_XML': DEFAULT_INTEGER,
        'SIST_IST': DEFAULT_INTEGER
    })
    default_kwargs = STRAINSVAR_Factory.default_kwargs.copy()
    default_kwargs.update(rzd_kwargs)
    Model = Change_STRAINSVAR_BUF

STRAINSVAR_BUF_Factory = Change_STRAINSVAR_BUF_Factory
create_strainsvar_buf = register_factory(STRAINSVAR_BUF_Factory)


class SRASPRP_Factory(ModelFactory, RawDataMixing):
    rzd_kwargs = default_kwargs = {
        'IDTR': DEFAULT_INTEGER,
        'IDR': DEFAULT_INTEGER,
        'CREG': DEFAULT_INTEGER,
        'SEQ': DEFAULT_INTEGER,
        'IDRP': DEFAULT_INTEGER,
        'NUMP': DEFAULT_INTEGER,
        'NUMO': DEFAULT_INTEGER,
        'PRIB': time(0),
        'OTPR': time(0),
        'KM': DEFAULT_FLOAT,
        'PRIBR': DEFAULT_INTEGER,
        'OTPRR': DEFAULT_INTEGER,
        'TEXST': DEFAULT_INTEGER,
    }


class Full_SRASPRP_Factory(SRASPRP_Factory):
    Model = Full_SRASPRP

create_full_srasprp = register_factory(Full_SRASPRP_Factory)


class Current_SRASPRP_Factory(SRASPRP_Factory):
    Model = Current_SRASPRP

create_current_srasprp = register_factory(Current_SRASPRP_Factory)


class Change_SRASPRP_BUF_Factory(SRASPRP_Factory):
    rzd_kwargs = SRASPRP_Factory.rzd_kwargs.copy()
    rzd_kwargs.update({
        'DATE_GVC': DEFAULT_DATE,
        'ID_XML': DEFAULT_INTEGER,
        'SIST_IST': DEFAULT_INTEGER
    })
    default_kwargs = SRASPRP_Factory.default_kwargs.copy()
    default_kwargs.update(rzd_kwargs)
    Model = Change_SRASPRP_BUF

SRASPRP_BUF_Factory = Change_SRASPRP_BUF_Factory
create_srasprp_buf = register_factory(SRASPRP_BUF_Factory)


class SCALENDAR_Factory(ModelFactory, RawDataMixing):
    rzd_kwargs = default_kwargs = {
        'IDTR': DEFAULT_INTEGER,
        'IDR': DEFAULT_INTEGER,
        'YEAR': DEFAULT_YEAR,
        'CDATE': DEFAULT_DATE,
        'OPER': DEFAULT_INTEGER,
    }

    def create_object(self, kwargs):
        kwargs['CDATE'] = build_date(kwargs.pop('CDATE', None))

        return super(SCALENDAR_Factory, self).create_object(kwargs)


class Full_SCALENDAR_Factory(SCALENDAR_Factory):
    Model = Full_SCALENDAR

create_full_scalendar = register_factory(Full_SCALENDAR_Factory)


class Current_SCALENDAR_Factory(SCALENDAR_Factory):
    Model = Current_SCALENDAR

create_current_scalendar = register_factory(Current_SCALENDAR_Factory)


class Change_SCALENDAR_BUF_Factory(SCALENDAR_Factory):
    rzd_kwargs = SCALENDAR_Factory.rzd_kwargs.copy()
    rzd_kwargs.update({
        'DATE_GVC': DEFAULT_DATE,
        'ID_XML': DEFAULT_INTEGER,
        'SIST_IST': DEFAULT_INTEGER
    })
    default_kwargs = SCALENDAR_Factory.default_kwargs.copy()
    default_kwargs.update(rzd_kwargs)
    Model = Change_SCALENDAR_BUF

SCALENDAR_BUF_Factory = Change_SCALENDAR_BUF_Factory
create_scalendar_buf = register_factory(SCALENDAR_BUF_Factory)


class SDOCS_Factory(ModelFactory, RawDataMixing):
    rzd_kwargs = default_kwargs = {
        'IDDOC': DEFAULT_INTEGER,
        'DOCTYPE': DEFAULT_INTEGER,
        'DOCNO': u'TEST DOCNO',
        'CDOR': DEFAULT_INTEGER,
        'DOCDATE': DEFAULT_DATE,
        'DOCINFO': u'TEST DOCINFO',
    }


class Full_SDOCS_Factory(SDOCS_Factory):
    Model = Full_SDOCS

create_full_sdocs = register_factory(Full_SDOCS_Factory)


class Current_SDOCS_Factory(SDOCS_Factory):
    Model = Current_SDOCS

create_current_sdocs = register_factory(Current_SDOCS_Factory)


class Change_SDOCS_BUF_Factory(SDOCS_Factory):
    rzd_kwargs = SDOCS_Factory.rzd_kwargs.copy()
    rzd_kwargs.update({
        'DATE_GVC': DEFAULT_DATE,
        'ID_XML': DEFAULT_INTEGER,
        'SIST_IST': DEFAULT_INTEGER
    })
    default_kwargs = SDOCS_Factory.default_kwargs.copy()
    default_kwargs.update(rzd_kwargs)
    Model = Change_SDOCS_BUF

SDOCS_BUF_Factory = Change_SDOCS_BUF_Factory
create_sdocs_buf = register_factory(SDOCS_BUF_Factory)


class IC00_STAN_Factory(ModelFactory, RawDataMixing):
    Model = IC00_STAN
    rzd_kwargs = default_kwargs = {
        'STAN_ID': DEFAULT_INTEGER,
        'DOR_KOD': DEFAULT_INTEGER,
        'PRED_ID': DEFAULT_INTEGER,
        'OKATO_ID': DEFAULT_INTEGER,
        'ST_KOD': DEFAULT_INTEGER,
        'VNAME': u'TEST VNAME',
        'NAME': u'TEST NAME',
        'STAN_TIP_ID': DEFAULT_INTEGER,
        'COR_TIP': u'TEST COR_TIP',
        'DATE_ND': DEFAULT_DATE,
        'DATE_KD': DEFAULT_DATE,
        'COR_TIME': u'TEST COR_TIME',
        'OPER_ID': DEFAULT_INTEGER,
        'REPL_FL': DEFAULT_INTEGER,
        'MNEM': u'TEST MNEM',
    }


create_ic00_stan = register_factory(IC00_STAN_Factory)


class Change_SPEC_BUF_Factory(ModelFactory, RawDataMixing):
    Model = Change_SPEC_BUF
    rzd_kwargs = default_kwargs = {
        'IDTR': DEFAULT_INTEGER,
        'KOP': DEFAULT_INTEGER,
        'VER': u'TEST VER',
        'DT': u'TEST DT',
        'USER': u'TEST USER',
        'DATE_GVC': DEFAULT_DATETIME,
        'ID_XML': DEFAULT_INTEGER,
        'SIST_IST': DEFAULT_INTEGER,
    }

    def create_object(self, kwargs):
        assert kwargs['update']
        assert kwargs['DATE_GVC']

        kwargs['DATE_GVC'] = build_datetime(kwargs.pop('DATE_GVC', None))
        return super(Change_SPEC_BUF_Factory, self).create_object(kwargs)


create_spec_buf = register_factory(Change_SPEC_BUF_Factory)
