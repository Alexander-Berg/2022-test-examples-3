# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

# based on
# better_yaml.py
# http://djangosnippets.org/snippets/2461/

from builtins import next
from collections import OrderedDict

import six
import yaml
from django.apps import apps
from django.conf import settings
from django.core.serializers import base
from django.core.serializers.pyyaml import DjangoSafeDumper, Serializer as YamlSerializer
from django.db import models, DEFAULT_DB_ALIAS
from django.utils.encoding import smart_text, smart_str
from six.moves import StringIO
from yaml.constructor import MappingNode, ConstructorError, SafeConstructor
from yaml.loader import Reader, Scanner, Parser, Composer, Constructor, Resolver


class Serializer(YamlSerializer):
    """
    Serialize database objects as nested dicts, indexed first by
    model name, then by primary key.
    """
    def start_serialization(self):
        self._current = None
        self.objects = {}

    def end_object(self, obj):
        model = smart_text(obj._meta)
        pk = obj._get_pk_val()

        if model not in self.objects:
            self.objects[model] = {}

        self.objects[model][pk] = self._current
        self._current = None

    def end_serialization(self):
        self.options.setdefault('default_flow_style', False)
        self.options.setdefault('encoding', 'utf8')
        self.options.setdefault('allow_unicode', True)

        yaml.dump(self.objects, self.stream, Dumper=DjangoSafeDumper, **self.options)


class Deserializer(six.Iterator):
    """
    Deserialize a stream or string of YAML data,
    as written by the Serializer above.
    """
    def __init__(self, stream_or_string, **options):
        self.stream_or_string = stream_or_string
        self.options = options

        if isinstance(stream_or_string, six.string_types):
            stream = StringIO(stream_or_string)
        else:
            stream = stream_or_string

        # Reconstruct the flat object list as PythonDeserializer expects
        # NOTE: This could choke on large data sets, since it
        # constructs the flattened data list in memory
        self.data = []
        self.depends = []

        for model, objects in yaml.load(stream, Loader=OrderedDictLoader).items():
            if model == 'depends':
                self.depends.extend(objects)
            else:
                # Add the model name back into each object dict
                for pk, fields in objects.items():
                    self.data.append({'model': model, 'pk': pk, 'fields': fields})

        self.deserialized = PythonDeserializer(self.data, **options)

    def __iter__(self):
        return self

    def __next__(self):
        return next(self.deserialized)


def PythonDeserializer(object_list, **options):  # noqa
    """
    Deserialize simple Python objects back into Django ORM instances.

    It's expected that you pass the Python objects themselves (instead of a
    stream or a string) to the constructor
    """
    db = options.pop('using', DEFAULT_DB_ALIAS)

    for d in object_list:
        # Look up the model and starting build a dict of data for it.
        model_class = _get_model(d["model"])

        data = dict()

        extra_data = {}

        if isinstance(d['pk'], six.string_types) and d["pk"].startswith('?'):
            # auto generate pk
            extra_data['pk'] = d["pk"][1:]
            if not extra_data['pk']:
                raise Exception("Not specified ref_pk '%s' must be '?<something>'" % d['pk'])
        else:
            data[model_class._meta.pk.attname] = model_class._meta.pk.to_python(d["pk"])

        m2m_data = {}

        # Handle each field
        for (field_name, field_value) in d["fields"].items():
            if isinstance(field_value, str):
                field_value = smart_text(field_value, options.get("encoding", settings.DEFAULT_CHARSET),
                                         strings_only=True)

            field = model_class._meta.get_field(field_name)

            # Handle M2M relations
            if field.rel and isinstance(field.rel, models.ManyToManyRel):
                if hasattr(field.rel.to._default_manager, 'get_by_natural_key'):
                    def m2m_convert(value):
                        if hasattr(value, '__iter__'):
                            return field.rel.to._default_manager.db_manager(db).get_by_natural_key(*value).pk
                        else:
                            return smart_text(field.rel.to._meta.pk.to_python(value))
                else:
                    def m2m_convert(value):
                        return smart_text(field.rel.to._meta.pk.to_python(value))

                m2m_data[field.name] = [m2m_convert(pk) for pk in field_value]

            # Handle FK fields
            elif field.rel and isinstance(field.rel, models.ManyToOneRel):
                if isinstance(field_value, six.string_types) and field_value.startswith('?'):
                    data[field.attname] = None
                    extra_data[field_name] = (field.rel.to, field_value[1:])
                elif field_value is not None:
                    if hasattr(field.rel.to._default_manager, 'get_by_natural_key'):
                        if hasattr(field_value, '__iter__'):
                            obj = field.rel.to._default_manager.db_manager(db).get_by_natural_key(*field_value)
                            value = getattr(obj, field.rel.field_name)
                            # If this is a natural foreign key to an object that
                            # has a FK/O2O as the foreign key, use the FK value
                            if field.rel.to._meta.pk.rel:
                                value = value.pk
                        else:
                            value = field.rel.to._meta.get_field(field.rel.field_name).to_python(field_value)
                        data[field.attname] = value
                    else:
                        data[field.attname] = field.rel.to._meta.get_field(field.rel.field_name).to_python(field_value)
                else:
                    data[field.attname] = None

            # Handle all other fields
            else:
                data[field.name] = field.to_python(field_value)

        do = base.DeserializedObject(model_class(**data), m2m_data)

        do.extra_data = extra_data
        do.original_data = {
            'model': model_class,
            'dict': data
        }

        yield do


class DeserializedObjectSaver(object):
    def __init__(self, abs_name, loaded_fixtures=None):
        self.loaded_objects = loaded_fixtures if loaded_fixtures is not None else {}
        self.loaded_fixtures = loaded_fixtures
        self.abs_name = abs_name
        self.base_module = abs_name.split(':')[0]

    def get_obj_from_fixtures(self, abs_name, model_name, pk):
        try:
            return self.loaded_fixtures[abs_name][model_name][pk]
        except KeyError:
            pass

        raise Exception('Unresolved link %s %s %s', abs_name, model_name, pk)

    def save(self, deserialized_object):
        do = deserialized_object

        model = do.object.__class__

        for attname, value in do.extra_data.items():
            if attname == 'pk':
                continue

            fk_model, fk_ref_pk = value
            model_name = u"%s.%s" % (fk_model._meta.app_label, fk_model._meta.model_name)

            if ':' in fk_ref_pk:
                fixture_name, fk_ref_pk = fk_ref_pk.split(':')

                if ':' not in fixture_name:
                    abs_name = '{}:{}'.format(self.base_module, fixture_name)
                else:
                    abs_name = fixture_name

                fk_obj = self.get_obj_from_fixtures(abs_name, model_name, fk_ref_pk)
            else:
                try:
                    fk_obj = self.loaded_objects[model_name][fk_ref_pk]
                except KeyError:
                    raise Exception('Unresolved link %s %s', model_name, fk_ref_pk)

            setattr(do.object, attname, fk_obj)

        try:
            do.save()
        except Exception:
            print(smart_str(u'Ошибка при сохранении фикстуры {}'.format(do.original_data)))
            raise

        if do.extra_data.get('pk'):
            ref_pk = do.extra_data.get('pk')
            model_name = u"%s.%s" % (model._meta.app_label, model._meta.model_name)

            self.loaded_objects.setdefault(model_name, {})[ref_pk] = do.object


def _get_model(model_identifier):
    """
    Helper to look up a model from an "app_label.model_name" string.
    """
    try:
        model = apps.get_model(*model_identifier.split("."))
    except TypeError:
        model = None
    if model is None:
        raise base.DeserializationError(u"Invalid model identifier: '%s'" % model_identifier)
    return model


class OrderedDictConstructor(Constructor):
    def construct_yaml_map(self, node):
        data = OrderedDict()
        yield data
        data.update(self.construct_mapping(node))

    def construct_mapping(self, node, deep=False):
        if isinstance(node, MappingNode):
            self.flatten_mapping(node)

        if not isinstance(node, MappingNode):
            raise ConstructorError(None, None,
                                   "expected a mapping node, but found %s" % node.id,
                                   node.start_mark)
        mapping = OrderedDict()
        for key_node, value_node in node.value:
            key = self.construct_object(key_node, deep=deep)
            try:
                hash(key)
            except TypeError as exc:
                raise ConstructorError("while constructing a mapping", node.start_mark,
                                       "found unacceptable key (%s)" % exc, key_node.start_mark)
            mapping[key] = self.construct_object(value_node, deep=deep)
        return mapping


# replace old constructor
OrderedDictConstructor.yaml_constructors = OrderedDictConstructor.yaml_constructors.copy()

for key, value in OrderedDictConstructor.yaml_constructors.items():
    if value == SafeConstructor.construct_yaml_map:
        OrderedDictConstructor.yaml_constructors[key] = OrderedDictConstructor.construct_yaml_map


class OrderedDictLoader(Reader, Scanner, Parser, Composer, OrderedDictConstructor, Resolver):

    def __init__(self, stream):
        Reader.__init__(self, stream)
        Scanner.__init__(self)
        Parser.__init__(self)
        Composer.__init__(self)
        OrderedDictConstructor.__init__(self)
        Resolver.__init__(self)
