from lib.app import db


class TestModel(db.Model):
    __tablename__ = 'test_stat' + 'test_model'
    id = db.Column(db.Integer, primary_key=True)
    test_string = db.Column(db.String(32))

    def __init__(self, test_string):
        self.test_string = test_string
