ngx.header.content_type = 'application/json; charset=UTF-8'
ngx.say('{"id":1,"name":"load001,"databases":[{"address":{"host":"man-arc5izadywspu1dq.db.yandex.net","port":6432,"dbname":"maildb","dataCenter":"MAN"},"role":"replica","status":"alive","state":{"lag":0}},{"address":{"host":"sas-w0ckruj66882d0hz.db.yandex.net","port":6432,"dbname":"maildb","dataCenter":"SAS"},"role":"replica","status":"alive","state":{"lag":0}},{"address":{"host":"vla-s8vr9k65cww07cb5.db.yandex.net","port":6432,"dbname":"maildb","dataCenter":"VLA"},"role":"master","status":"alive","state":{"lag":0}}]}')
ngx.exit(200)
