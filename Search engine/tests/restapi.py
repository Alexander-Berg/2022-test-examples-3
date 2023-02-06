import datetime as dt

import server.restapi

import mongoengine as me


class Class(me.Document):
    class Embedded(me.EmbeddedDocument):
        intfield = me.IntField(min_value=1)
        decfield = me.DecimalField(min_value=0)
        dtfield = me.DateTimeField()

    id = me.StringField(primary_key=True, max_length=64)
    ref = me.ReferenceField("Class")
    embedded = me.EmbeddedDocumentField(Embedded)


def test_model_to_response():
    obj = Class()
    data = server.restapi.model2response(obj)
    assert data == {}

    obj.id = "SOME_ID"
    obj.ref = "SOME_ID"
    obj.embedded = Class.Embedded()
    data = server.restapi.model2response(obj)
    assert data['id'] == "SOME_ID"
    assert data['ref'] == "SOME_ID"
    assert data['embedded'] == {}

    now = dt.datetime.utcnow()
    obj.embedded.intfield = 0
    obj.embedded.decfield = 3.14  # 3data
    obj.embedded.dtfield = now

    data = server.restapi.model2response(obj)
    assert data['embedded']['intfield'] == obj.embedded.intfield
    assert data['embedded']['decfield'] == obj.embedded.decfield
    assert data['embedded']['dtfield'] == now.isoformat() + 'Z'
