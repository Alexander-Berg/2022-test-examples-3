#-*- coding: utf-8 -*-
import os
import re
import json
import sys
import locale
import codecs
import Cookie
import calendar
import datetime
import urllib2
import memcache

from jinja2 import Environment, FileSystemLoader, ChoiceLoader

reload(sys)
sys.setdefaultencoding('utf-8')

FORWARD_NUMBER  = '1601'
FORWARD_API     = "http://bot.yandex-team.ru/api/changeForward.php"
STAFF_API       = "http://api.staff.yandex-team.ru/v3/"
# Will skip this users in possible on-call admins
DIRECT_ADMINS   = ['techpriest','rivik','dspushkin']

def get_with_cookie(url, cookie):
    opener = urllib2.build_opener()
    opener.addheaders.append(('Cookie','Session_id=%s' % cookie))
    data=opener.open(url)
    return data.read()

def release_message():
    today   = int(datetime.datetime.now().weekday())
    hour    = int(datetime.datetime.now().strftime('%H'))

    locale.setlocale(locale.LC_ALL,'ru_RU.UTF8')
    day_of_week = calendar.day_name[today].lower()
    msg=''
    if today > 3:
        msg += '<h2>Сегодня %s, а значит ваш пакет в продашн поедет только на следующей неделе.</h2>' % day_of_week
    elif today == 3 and hour > 19:
        msg += '<h2>Сейчас уже позже 7 часов вечера четверга, все тикеты в продакшн будут выложены в понедельник.</h2>'
    elif hour > 19 or hour < 11:
        msg += '<h2>Уже больше 7 часов вечера, все тикеты будут выложены завтра.</h2>'
    return msg

def duty(environ, start_response):
    # Get session_id from request (w/o checks)
    cookies=Cookie.SimpleCookie()
    cookies.load(environ['HTTP_COOKIE'])
    sess_id=cookies['Session_id'].value

    memc = memcache.Client(['127.0.0.1:11211'], debug=1);

    if environ['QUERY_STRING']:
        (action, param) = environ['QUERY_STRING'].split('=')
        # Change on-call user
        if action == 'set_duty':
            # Invalidate all caches
            memc.delete('duty_data')
            memc.delete('group_users')
            
            # Get new on-call user phone and update forward
            url=STAFF_API+'/persons?_fields=login,work_phone,name&login='+param
            data = json.loads(get_with_cookie(url,sess_id))
            duty_phone = data['result'][0]['work_phone']
            url = "%s?num=%s&fnum=55%d" % (FORWARD_API, FORWARD_NUMBER, duty_phone)
            urllib2.urlopen(url)

            status = '302 Moved Temporarily'
            response_headers = [
                ('Location','/'),
            ]
            start_response(status, response_headers)
            return ''

    m_result = memc.get('duty_data')
    if not m_result:
        # Find current forward numer:
        url = FORWARD_API+"/?getForward&num="+FORWARD_NUMBER
        current_forward= str(urllib2.urlopen(url).read())

        # Strip 55 prefix
        if len(current_forward) == 6:
            current_forward = current_forward[2:6]
    
        # Find on-call user data by forward phone:
        url=STAFF_API+'/persons?_fields=login,work_phone,name&work_phone='+current_forward
        duty_data = json.loads(get_with_cookie(url,sess_id))
 
        memc.set('duty_data',duty_data,600)
    else:
        duty_data = m_result

    m_result = (memc.get('group_users'),memc.get('possible_users'))
    if not m_result[0] or not m_result[1]:
        # Find users in group:
        url=STAFF_API+'/groupmembership?_fields=person&group.url=' + 'yandex_mnt_advtech_rt'
        possible_users=json.loads(get_with_cookie(url,sess_id))
    
        group_users=[ x['person']['login'] for x in possible_users['result'] \
                        if x['person']['official']['is_dismissed'] == False \
                        and x['person']['login'] not in DIRECT_ADMINS \
                        and x['person']['login'] != duty_data['result'][0]['login'] ]

        memc.set('group_users',group_users,3600)
        memc.set('possible_users',possible_users,3600)
    else:
        group_users,possible_users=m_result
    
    add_msg = release_message()
    
    # Render template
    env     = Environment( loader=FileSystemLoader('/usr/lib/duty/templates-test', encoding='utf-8'), extensions=["jinja2.ext.do",] )
    tpl     = env.get_template( "index.tpl" )
    data    = tpl.render( duty=duty_data, additional_message = add_msg, users=group_users , user=cookies['yandex_login'].value, acl=possible_users)
    
    status = '200 OK'
    response_headers = [
        ('Content-type','text/html; charset=utf-8'),
        # Doesn't work with UTF
        # ('Content-Length', str(len(data)))
    ]
    start_response(status, response_headers)
    return iter([data.encode("UTF-8")])


