# coding: utf-8


class User(object):
    def __init__(self, uid, local, domain, display_name):
        self.uid = uid
        self.local = local
        self.domain = domain
        self.display_name = display_name


class Label(object):
    def __init__(self, name="test_label", label_type="user"):
        self.name = name
        self.label_type = label_type


class MimePart(object):
    def __init__(self, hid="1", content_type="text", content_subtype="plain", boundary="", name="", charset="US-ASCII",
                 encoding="7bit", content_disposition="", filename="", cid="", offset=0, length=1024):
        self.hid = hid
        self.content_type = content_type
        self.content_subtype = content_subtype
        self.boundary = boundary
        self.name = name
        self.charset = charset
        self.encoding = encoding
        self.content_disposition = content_disposition
        self.filename = filename
        self.cid = cid
        self.offset = offset
        self.length = length


class Attachment(object):
    def __init__(self, hid="1", attachment_type="image/jpeg", filename="img", size=512):
        self.hid = hid
        self.attachment_type = attachment_type
        self.filename = filename
        self.size = size


class ThreadMeta(object):
    def __init__(self, merge_rule="force_new", hash_namespace="subject", reference_hashes=None):
        self.merge_rule = merge_rule
        self.hash_namespace = hash_namespace
        self.reference_hashes = reference_hashes if reference_hashes else []


class MessageData(object):
    def __init__(self, from_user, recipients):
        self.from_user = from_user
        self.recipients = recipients
        self.subject = "subject"
        self.stid = "320.mail:0.E3960:123456789"
        self.received_date = 1519054543
        self.firstline = "fistline"
        self.message_id = "12345"
        self.size = 1024
        self.lids = {}
        self.labels = {}
        self.label_symbols = {}
        self.mime_parts = [MimePart()]
        self.attachments = [Attachment()]
        self.thread_meta = ThreadMeta()
        self.folder_path = {}
        self.no_such_folder_action = {}
        self.ignore_duplicates = {}
        self.remove_duplicates = {}
        self.imap = {}
        self.fid = {}
        self.stids = {}
        self.offset_diffs = {}


class Symbols(object):
    class Item():
        def __init__(self, code):
            self.Code = code

    NONE = Item(0)
    INBOX = Item(1)
    SENT = Item(2)
    SPAM = Item(4)
    DRAFT = Item(5)
    ARCHIVE = Item(7)
    PENDING = Item(11)
