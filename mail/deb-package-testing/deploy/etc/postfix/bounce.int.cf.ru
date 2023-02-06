success_template = <<EOF
Charset: utf-8
From: mailer-daemon@yandex.ru
Subject: =?UTF-8?B?0J/QuNGB0YzQvNC+INGD0YHQv9C10YjQvdC+INC00L7RgdGC0LDQstC70LXQvdC+?=

              **********

Ваше письмо было успешно доставлено указанному адресату (или адресатам,
если было указано несколько).
В случае возникновения проблемы на принимающей стороне Вы получите
отдельное уведомление от другой почтовой системы.

Это уведомление автоматически отправлено почтовой системой Яндекса.

              **********

This is the mail system at host $mydomain.

Your message was successfully delivered to the destination(s)
listed below. If the message was delivered to mailbox you will
receive no further notifications. Otherwise you may still receive
notifications of mail delivery errors from other systems.

EOF

failure_template = <<EOF
Charset: utf-8
From: mailer-daemon@yandex.ru
Subject: =?UTF-8?B?0J3QtdC00L7RgdGC0LDQstC70LXQvdC90L7QtSDRgdC+0L7QsdGJ0LXQvdC40LU=?=

              **********

Это письмо отправлено почтовым сервером $mydomain.

К сожалению, мы вынуждены сообщить Вам о том, что Ваше письмо не может
быть отправлено одному или нескольким адресатам. Технические подробности можно найти ниже.

Возможные причины недоставки указаны по адресу:
https://yandex.ru/support/mail-new/wizard/zout_send/not-got-report-yes-yandex.html

Пожалуйста, не отвечайте на это сообщение. 

              **********

This is the mail system at host $mydomain.

I'm sorry to have to inform you that your message could not
be delivered to one or more recipients. It's attached below.

You can find the the possible reasons of the undelivered message letter here:
https://yandex.com/support/mail-new/wizard/zout_send/not-got-report-yes-yandex.html

Please, do not reply to this message.

EOF

