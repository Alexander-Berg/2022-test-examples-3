import json
import logging

import pytest
from flask import Flask

from load.projects.firestarter.src.routes import bp
from load.projects.firestarter.src.config import Config


class TestConfig(Config):
    db_config = {'host': 'localhost', 'port': 6379, 'name': 4}


def create_app(config=Config):
    logging.basicConfig(level=logging.DEBUG,
                        format='%(asctime)s - %(name)s -  %(filename)s:%(lineno)d - %(levelname)s %(message)s')

    app = Flask(__name__)
    app.config.from_object(config)

    app.register_blueprint(bp)

    return app


@pytest.fixture
def test_client():
    app = create_app(config=TestConfig)
    app.config['TESTING'] = True

    with app.test_client() as client:
        with app.app_context():
            # db = init_db(TestConfig.db_config)
            yield client


class TestStatusHandler:

    def test_empty_request(self, test_client):
        rv = test_client.get('/firestarter/status')
        assert rv.status_code == 400
        assert json.loads(rv.data) == {'errors': 'Bad request'}

    def test_wrong_method(self, test_client):
        rv = test_client.post('/firestarter/status')
        assert rv.status_code == 405

    def test_incorrect_id(self, test_client):
        rv = test_client.get('/firestarter/status?id=5+6')
        assert rv.status_code == 400
        assert json.loads(rv.data) == {'errors': 'Bad request'}

    @pytest.mark.skip
    def test_nonexistent_id(self, test_client):
        rv = test_client.get('/firestarter/status?id=aaa')
        assert rv.status_code == 404
        assert json.loads(rv.data) == {'errors': 'Id not found'}

    def test_existent_id(self, test_client):
        rv = test_client.post('/firestarter/start')
        assert rv.status_code == 400
