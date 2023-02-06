# coding: utf-8

# import doctest
from parse import with_pattern
from parse_type import TypeBuilder
import yaml
import hamcrest
from pymdb import helpers
from pymdb.types import Serials, SubscriptionAction, FolderArchivationType, ContactsUserType

from tests_common.pytest_bdd import BehaveParser


@with_pattern(r'\$[\w-]+')
def parse_mid(text):
    return text


@with_pattern(r'((\$\w+[\s,]*)+)|(\$\w*\[\d+:\d+\]\w*)')
def parse_mids_range(text):
    return helpers.parse_values(text)


@with_pattern(r'(\$\w*\[\d+:\d+\]\w*)')
def parse_id_range(text):
    return helpers.parse_values(text)


@with_pattern(r'\$[\w-]+')
def parse_op_id(text):
    return text


# in some cases name can be empty
@with_pattern(r'[\w-]*')
def parse_name(text):
    return text


@with_pattern(r'[\w-]+')
def parse_dashed_word(text):
    return text


@with_pattern(r'[\w-]+([\s\w-]+)?')
def parse_user_name(text):
    return text


@with_pattern(r'enabled|disabled')
def parse_enabled(text):
    return text == 'enabled'


@with_pattern(r'verified|not verified')
def parse_verified(text):
    return text == 'verified'


@with_pattern(r'has|does not have')
def parse_has(text):
    return text == 'has'


@with_pattern(r'\w+\.\w+')
def parse_table_name(text):
    return text


@with_pattern(r'[\w./-]+')
def parse_path(text):
    return text


@with_pattern(r'[^"]+')
def parse_yaml_value(text):
    return yaml.safe_load(text)


parse_matcher = TypeBuilder.make_enum({
    u'is less than': hamcrest.less_than,
    u'is greater than': hamcrest.greater_than,
    u'equals to': hamcrest.equal_to,
})


class FolderRef(object):
    def __init__(self, user_name, folder_type, folder_name):
        self.user_name = user_name
        self.folder_name = folder_name
        self.folder_type = folder_type

    def __str__(self):
        '''
        >>> str(FolderRef('vasia', folder_type=None, folder_name='Junk'))
        'name:Junk@vasia'
        >>> str(FolderRef('olia', folder_type='inbox', folder_name=None))
        'inbox@olia'
        '''
        folder_desc = self.folder_type
        if self.folder_name:
            folder_desc = 'name:' + self.folder_name
        return folder_desc + '@' + self.user_name

    def __repr__(self):
        return '<name={0.folder_name}, type={0.folder_type}, user={0.user_name}>'.format(self)

    @classmethod
    @with_pattern(r'(name\:)?[\w-]+@[\w-]+')
    def parse(cls, text):
        '''
        >>> FolderRef.parse('name:Junk@vasia')
        <name=Junk, type=None, user=vasia>
        >>> FolderRef.parse('inbox@kolia')
        <name=None, type=inbox, user=kolia>
        >>> FolderRef.parse('name:Fancy-Junk@olia')
        <name=Fancy-Junk, type=None, user=olia>
        '''
        folder_desc, user_name = text.split('@')
        folder_type = folder_name = None
        if folder_desc.startswith('name:'):
            folder_name = folder_desc[folder_desc.find(':')+1:]
        else:
            folder_type = folder_desc
        return cls(
            user_name=user_name,
            folder_type=folder_type,
            folder_name=folder_name
        )


@with_pattern(r'(synced )?revision \"\d+\"')
def parse_subscriber_folder_attribute(text):
    revision = int(text.split()[-1].strip(u'"'))
    if text.startswith(u'synced revision'):
        return hamcrest.has_property(
            'synced_revision', revision
        )
    elif text.startswith('revision'):
        return hamcrest.has_property(
            'revision', revision
        )
    raise RuntimeError(
        'Unexpected text %s for subscriber folder' % text.encode('utf-8')
    )


parse_subscriber_folder_attributes = TypeBuilder.with_many(
    parse_subscriber_folder_attribute,
    listsep="and"
)


def nullable_int(text):
    try:
        return int(text)
    except ValueError:
        return None


def extra_parsers():
    return dict(
        OpID=parse_op_id,
        Mid=parse_mid,
        Variable=parse_mid,
        MidsRange=parse_mids_range,
        IdRange=parse_id_range,
        Name=parse_name,
        UserName=parse_user_name,
        DashedWord=parse_dashed_word,
        Enabled=parse_enabled,
        Verified=parse_verified,
        TableName=parse_table_name,
        FilePath=parse_path,
        FolderRef=FolderRef.parse,
        YAML=parse_yaml_value,
        Matcher=parse_matcher,
        IntOrNone=nullable_int,
        SubscriptionAction=TypeBuilder.make_enum(SubscriptionAction),
        SubscriptionActionAndMore=TypeBuilder.with_many(TypeBuilder.make_enum(SubscriptionAction), listsep=","),
        SubscribedMatcher=parse_subscriber_folder_attribute,
        SubscribedMatcherAndMore=parse_subscriber_folder_attributes,
        SerialColumn=TypeBuilder.make_choice(Serials.__slots__),
        ArchivationType=TypeBuilder.make_enum(FolderArchivationType),
        ChangeLogAttribute=TypeBuilder.make_choice(['changed', 'arguments']),
        ContactsUserType=TypeBuilder.make_enum(ContactsUserType),
        NameList=TypeBuilder.with_many(parse_name, listsep=","),
        Has=parse_has,
    )


BehaveParser.extra_types.update(extra_parsers())
