import os
from os.path import dirname, join
from django.core.files.uploadedfile import InMemoryUploadedFile
from fan.message.letter import CONTENT_TYPE_HTML, CONTENT_TYPE_ZIP, CONTENT_TYPE_PNG


def load_test_letter(filepath):
    letter_file = open(
        join(dirname(dirname(dirname(__file__))), "fan_ui/tests/api/data/letter", filepath), "rb"
    )
    filename = filepath.split("/")[-1]
    if filename.endswith(".zip"):
        content_type = CONTENT_TYPE_ZIP
    elif filename.endswith(".png"):
        content_type = CONTENT_TYPE_PNG
    else:
        content_type = CONTENT_TYPE_HTML
    return InMemoryUploadedFile(
        file=letter_file,
        field_name=None,
        name=filename,
        content_type=content_type,
        size=os.fstat(letter_file.fileno()).st_size,
        charset="utf-8",
    )


def get_html_body(email_message):
    for part in email_message.walk():
        if part.get_content_type() == "text/html":
            return part.get_payload(None, True).decode("utf-8")
    return None


def attachments_filelist(letter):
    return [attach.filename for attach in letter.attachments.all()]


def attachments_file_sizes(letter):
    return [attach.file_size for attach in letter.attachments.all()]
