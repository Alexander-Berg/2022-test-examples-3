# -*- coding: utf-8 -*-
import os
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from extensions.configurations.sfront.model import Base, RequestsConfiguration, Sources, ExtendedSources


def test_requests_add(session):
    configuration = RequestsConfiguration(location="MSK", region="RKUB", project="WEB", environment="PRODUCTION")
    source1 = Sources()
    source2 = ExtendedSources()
    session.add(configuration)
    session.add(source1)
    session.add(source2)
    configuration.sources.append(source1)
    configuration.sources.append(source2)
    session.rollback()


def test_requests_read(session):
    assert len(session.query(RequestsConfiguration).all())


@pytest.fixture
def engine(test_data_dir):
    db_path = os.path.join(test_data_dir, "configurations", "request.db")
    engine = create_engine('sqlite:///' + db_path)
    Base.metadata.reflect(engine)
    return engine


@pytest.fixture
def session(engine):
    Base.metadata.bind = engine
    DBSession = sessionmaker(bind=engine)
    session = DBSession()
    return session
