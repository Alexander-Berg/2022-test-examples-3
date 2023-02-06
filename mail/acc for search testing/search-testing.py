import os
import smtplib
from dataclasses import dataclass
from email import encoders
from email.mime.base import MIMEBase
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from typing import List

attachments = ['с вложениями', 'без вложений']
folders = ['Входящие', 'Пользовательская папка', 'Пользовательская подпапка', 'Отправленные', 'Удаленные', 'Спам', 'Черновики', 'Шаблоны', 'Архив']
read_status = ['непрочитанное', 'прочитанное']
importance_type = ['важное', 'неважное']
scopes = ['в теме', 'в тексте', 'в теме и в тексте']
from_addr = ['f.anem@yandex.ru', 'qwerty123asdf']
to_addrs = [['test.fanem@yandex.ru', 'advanced-search-test@yandex.ru']]


@dataclass(frozen=True)
class Smtp:
    domain = 'smtp.yandex.ru'
    port = 587


def create_message(subject: str, body: str, to_addr: List[str], attachment: bool) -> str:
    msg = MIMEMultipart('alternative')
    msg['Subject'] = subject
    msg['From'] = from_addr[0]
    msg['To'] = ','.join(to_addr)
    msg.attach(MIMEText(str(body), 'html', 'utf-8'))

    if attachment:
        file = "text.txt"
        attachment = MIMEBase('application', 'octet-stream')
        attachment.set_payload(open(file, 'rb').read())
        encoders.encode_base64(attachment)
        attachment.add_header('Content-Disposition', 'attachment; filename="%s"' % os.path.basename(file))
        msg.attach(attachment)

    return msg.as_string()


def send(msg, to_addr: List[str]) -> None:
    try:
        smtp_object = smtplib.SMTP(Smtp.domain, Smtp.port)
        smtp_object.ehlo()
        smtp_object.starttls()
        smtp_object.login(from_addr[0], from_addr[1])
        smtp_object.sendmail(from_addr=from_addr[0], to_addrs=to_addr, msg=msg)
        print(f'Successfully sent email to: {to_addr}')
    except RuntimeError as err:
        print(err)


def main():
    for folder in folders:
        for status in read_status:
            for type in importance_type:
                for scope in scopes:
                    for attachment in attachments:
                        for to_addr in to_addrs:
                            text = f'Папка: {folder}, прочитанность: {status}, важность: {type}, скоуп: {scope};'
                            if scope == 'в теме':
                                subj, body = text, ''
                            elif scope == 'в тексте':
                                subj, body = '', text
                            else:
                                subj, body = text, text

                            msg = create_message(subject=subj, body=body, to_addr=to_addr, attachment=attachment == 'с вложениями')
                            send(msg=msg, to_addr=to_addr)


if __name__ == '__main__':
    main()
