import logging
import flask
from flask import jsonify

from lib.app import db
from lib.data.test_stat_models import TestModel

logger = logging.getLogger()
app = flask.Blueprint('test_stat_api', __name__)


@app.route('/api/test-stat/db/create', methods=['POST'])
def handle_create_test_stat_db():
    engine = db.session.get_bind()
    TestModel.__table__.create(engine)
    return jsonify(result=True)


@app.route('/api/test-stat/db/drop', methods=['POST'])
def handle_drop_test_stat_db():
    engine = db.session.get_bind()
    TestModel.__table__.drop(engine)
    return jsonify(result=True)


@app.route('/api/test-stat/add')
def handle_test_stat_ping():
    return 'hello, world!'
