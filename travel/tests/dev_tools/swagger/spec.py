# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from marshmallow import Schema, fields
from rest_framework.decorators import api_view


@api_view(['GET'])
def some_view(request):
    pass


class SomeSchema(Schema):
    foo = fields.String()
    bar = fields.Number()
