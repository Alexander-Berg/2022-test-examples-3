import jsonschema


def create_default_values_setter(validator_class):
    '''
    All the validators (except the one for "properties") will be effectively removed.
    During the call to `validate` method the "properties" validator will only set
    default values for the missed properties (and will not actually validate anything).
    '''
    def set_defaults(validator, properties, instance, schema):
        for property_, subschema in properties.items():
            if "default" in subschema:
                value = subschema['default']
                if isinstance(value, unicode):
                    value = value.encode('utf-8')
                instance.setdefault(property_, value)

    def noop(*args, **kws):
        pass

    validators = {key : noop for key in validator_class.VALIDATORS}
    validators.update({"properties": set_defaults})
    return jsonschema.validators.extend(validator_class, validators)


def assign_default_values_to_missed_properties(test_cfg, schema):
    default_values_setter_cls = create_default_values_setter(jsonschema.Draft7Validator)
    default_values_setter = default_values_setter_cls(schema)
    default_values_setter.validate(test_cfg)
