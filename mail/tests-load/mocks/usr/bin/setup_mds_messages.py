#! /usr/bin/env python3

import string
import random
import datetime
from email.mime.text import MIMEText


def gen_str(size, with_newlines=False):
    source = string.ascii_letters + string.digits
    if with_newlines:
        source = source + "\n"
    return "".join([random.choice(source) for x in range(size)])


def gen_date():
    start = datetime.datetime(2015, 1, 1)
    end = datetime.datetime(2019, 1, 1)
    delta = end - start
    random_day = random.randrange(delta.days if delta.days > 0 else 100)
    random_second = ((random_day + 1) * 24 * 60 * 60) + random.randrange(24 * 60 * 60)
    res = start + datetime.timedelta(seconds=random_second)
    return res


def gen_message(msg_size):
    msg = MIMEText(gen_str(msg_size, with_newlines=True), _charset="utf-8")
    msg["Subject"] = gen_str(50)
    msg["From"] = gen_str(10) + "@gmail.com"
    msg["To"] = gen_str(10) + "@mail.ru"
    msg["Message-Id"] = gen_str(100)
    msg["Date"] = gen_date().strftime("%a, %d %b %Y %H:%M:%S +0300")
    msg["Content-Type"] = "text/html; charset=utf-8"
    return msg.as_string()


messages_distribution = [
    {"percentile": 50, "size": 33},
    {"percentile": 90, "size": 166},
    {"percentile": 95, "size": 413},
    {"percentile": 100, "size": 2000},
]

FOLDER = "/var/www/static/"

messages_lua_header = """
module("messages", package.seeall)
_VERSION = '0.01'

message_file_pattern = "/{size}KB"

messages_distribution = {
"""
messages_lua_footer = """
}

function select_message()
    current_message = math.random(1,100);
    for i, msg in ipairs(messages_distribution) do
        if (current_message <= msg.percentile) then
            return message_file_pattern:gsub("{size}", msg.size)
        end
     end
end
"""

lua_distribution_entry = "{{percentile={perc}, size={size}}}"

lua_distributions = []

for entry in messages_distribution:
    msg = gen_message(entry["size"] * 1024)
    file = FOLDER + str(entry["size"]) + "KB.eml"
    with open(file, "w") as out:
        out.write(msg + "\n")

    lua_distributions.append(
        lua_distribution_entry.format(perc=entry["percentile"], size=entry["size"])
    )

messages_lua = messages_lua_header + ",".join(lua_distributions) + messages_lua_footer
with open("/var/www/lua/common/messages.lua", "w") as out:
    out.write(messages_lua)
