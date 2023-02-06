import yatest.common

'''
data generation

def input_data():
    from market.proto.MboParameters_pb2 import (
        Category as MboCategory,
        Parameter as MboParameter,
        Option as MboOption,
        EnumAlias as MboAlias,
        Word as MboWord
    )

    def word(name, lang_id=225):
        return MboWord(name=name, lang_id=lang_id)

    def alias(name, lang_id=225, type=MboAlias.GENERAL):
        return MboAlias(alias=word(name, lang_id), type=type)

    def name_param(param_id):
        return "Parameter {}".format(param_id)

    def name_param_xsl(param_id):
        return "Parameter_xsl {}".format(param_id)

    def name_opt(opt_id):
        return "opt:{}".format(opt_id)

    def name_alias(opt_id, alias_name):
        return "alias:{}:{}".format(opt_id, alias_name)

    def rus_option(opt_id, alias, name=None, published=True):
        if name is None:
            name = [word(name_opt(opt_id))]

        return MboOption(
            id=opt_id,
            name=name,
            alias=alias,
            published=published
        )

    def mbo_parameter(id, options, name=None, published=True):
        if name is None:
            name = [word(name_param(id))]

        return MboParameter(
            id=id,
            xsl_name=name_param_xsl(id),
            name=name,
            option=options,
            published=published
        )

    return [
        MboCategory(
            hid=10,
            parameter=[
                mbo_parameter(
                    id=1,
                    options=[
                        # good name and some alias
                        rus_option(1001, [alias(name_opt(1001)), alias(name_alias(1001, 1))]),
                        # no name alias, good alias
                        rus_option(1002, [alias(name_alias(1002, 1))]),
                        # no good name
                        rus_option(
                            1003,
                            [alias(name_opt(1003), lang_id=1), alias(name_alias(1006, 1), lang_id=1)],
                            name=[word(name_opt(1006), lang_id=1)]
                        )
                    ]
                ),
                # parameter with xsl_name only
                mbo_parameter(
                    id=2,
                    options=[
                        # two bad aliases
                        rus_option(1004, [alias(name_opt(1004), lang_id=1), alias(name_opt(1004), type=MboAlias.SEARCH), alias(name_alias(1004, 1))]),
                        # bad name language
                        rus_option(
                            1005,
                            [alias(name_opt(1005), lang_id=1), alias(name_opt(1005), type=MboAlias.SEARCH), alias(name_alias(1005, 1))],
                            name=[word(name_opt(1005), lang_id=1)]
                        ),
                        # bad name language, good first alias
                        rus_option(
                            1006,
                            [alias(name_opt(1006)), alias(name_opt(1006), type=MboAlias.SEARCH), alias(name_alias(1006, 1))],
                            name=[word(name_opt(1006), lang_id=1)]
                        )
                    ],
                    name=[]
                )
            ]
        ),
        MboCategory(
            hid=20,
            parameter=[
                mbo_parameter(
                    id=1,
                    options=[
                        # good name and some alias
                        rus_option(1010, [alias(name_opt(1010)), alias(name_alias(1010, 1))]),
                        # not published
                        rus_option(1019, [alias(name_opt(1019)), alias(name_alias(1019, 1))], published=False),
                    ]
                ),
                # not published
                mbo_parameter(
                    id=2,
                    options=[
                        # good name and some alias
                        rus_option(1018, [alias(name_opt(1018)), alias(name_alias(1018, 1))]),
                    ],
                    published=False
                ),
                # bad parameter id
                mbo_parameter(
                    id=5,
                    options=[
                        # good name and some alias
                        rus_option(1017, [alias(name_opt(1017)), alias(name_alias(1017, 1))]),
                    ]
                ),
            ]
        ),
    ]


def create_mbo_file(path, data):
    from market.pylibrary.mbostuff.utils.mbo_proto_write import MboOutputStream
    mbo_stream = MboOutputStream(path, "MBOC")
    for d in data:
        mbo_stream.write(d.SerializeToString())
'''


def dump(input, output, format):
    command = [
        yatest.common.binary_path("market/idx/tools/pbsncat/bin/pbsncat"),
        '--input-format', format,
        input,
    ]

    with open(output, "w") as out:
        yatest.common.execute(command, stdout=out)


def test_proto_names():
    DATA_FILENAME = yatest.common.source_path("market/tools/getter_validators/mbo-params-names/tests/data/data.pb")
    NAMES_FILENAME = "names.pbuf.sn"
    TEXT_DUMP_FILENAME = "names.txt"
    DATA_DUMP_FILENAME = "input.txt"

    command_run = [
        yatest.common.binary_path("market/tools/getter_validators/mbo-params-names/mbo-params-names"),
        '--input', DATA_FILENAME,
        '--output', NAMES_FILENAME,
        '--parameters', '1,2,17',
    ]
    yatest.common.execute(command_run)

    dump(NAMES_FILENAME, TEXT_DUMP_FILENAME, 'pbsn')
    dump(DATA_FILENAME, DATA_DUMP_FILENAME, 'mbo-pb')

    return [
        yatest.common.canonical_file(DATA_DUMP_FILENAME, diff_tool_timeout=60),
        yatest.common.canonical_file(TEXT_DUMP_FILENAME, diff_tool_timeout=60),
    ]
