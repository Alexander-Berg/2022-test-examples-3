#pragma once

static const string JSON_TASK = R"({
    "uid": "123",
    "suid": "456",
    "events": [{
        "change_id": 123,
        "action": "move",
        "items": [
            {"deleted":false,"fid":37,"lids":[9,22],"mid":111,"recent":true,"seen":true,"src_fid":1,"src_tab":null,"tab":null,"srcTab": null,"tid":111},
            {"deleted":false,"fid":37,"lids":[9,22],"mid":222,"recent":true,"seen":true,"src_fid":1,"src_tab":null,"tab":null,"srcTab": null,"tid":222}
        ],
        "args": {"fid": 37},
        "ts": 123,
        "lcn": "789"
    }, {
        "change_id": 126,
        "action": "status",
        "items": [
            {"fid": 47, "mid": 333, "tab": null, "srcTab": null, "tid": 333, "lids": [9, 10], "seen": true, "recent": true, "deleted": false}
        ],
        "args": {"seen": true, "recent": null, "deleted": null, "lids_add": [], "lids_del": []},
        "ts": 124,
        "lcn": "889"
    }, {
        "change_id": 127,
        "action": "store",
        "items": [
            {"deleted":false,"fid":37,"hdr_message_id":"<smth>","lids":[12],"mid":444,"recent":true,"seen":false,"tab":null,"tid":444}
        ],
        "args": {},
        "ts": 125,
        "lcn": "999"
    }]
})";

static const string ENVELOPE_111 = R"({
    "mid":"111",
    "fid":"47",
    "threadId":"111",
    "revision":11,
    "date":1378377600,
    "receiveDate":0,
    "from":[
      {
        "local":"user",
        "domain":"domain.ru",
        "displayName":"Jack Daniels"
      }
    ],
    "subject":"Hi Jack",
    "hdrStatus":"RO",
    "to":[
      {
        "local":"user2",
        "domain":"domain2.ru",
        "displayName":"Test User"
      }
    ],
    "firstline":"test test",
    "size":2087,
    "newCount":0,
    "attachmentsCount":0,
    "attachmentsFullSize":0,
    "attachments":[],
    "labels":[
      "2170000000009092243",
      "2170000250000042541",
      "FAKE_HAS_USER_LABELS_LBL",
      "FAKE_SEEN_LBL"
    ],
    "specialLabels":[],
    "types":[],
    "tab":"default"
})";

static const string ENVELOPE_444 = R"({
"mid":"444",
"fid":"47",
"threadId":"111",
"revision":11,
"date":1378377600,
"receiveDate":0,
"from":[
  {
    "local":"user",
    "domain":"domain.ru",
    "displayName":"Jack Daniels"
  }
],
"subject":"Hi Jack",
"hdrStatus":"RO",
"to":[
  {
    "local":"user2",
    "domain":"domain2.ru",
    "displayName":"Test User"
  }
],
"firstline":"test test",
"size":2087,
"newCount":0,
"attachmentsCount":0,
"attachmentsFullSize":0,
"attachments":[],
"labels":[
  "2170000000009092243",
  "2170000250000042541",
  "FAKE_HAS_USER_LABELS_LBL",
  "FAKE_SEEN_LBL"
],
"specialLabels":[],
"types":[],
"tab":"default"
})";

static const string SUB_LIST = R"([{
  "client": "test_client",
  "filter": "",
  "id": "870a4d3070ff6319119946ef3e25049953fc2c87",
  "session": "session_abcde",
  "ttl": 8,
  "url": "http://sample/callback"
}])";

static const string COUNTERS = R"({"folders":{"4":{"cnt":0,"new":0},
  "5":{"cnt":7,"new":3},"7":{"new":0,"cnt":0},"2":{"cnt":0,"new":0},
  "6":{"cnt":3,"new":0},"3":{"cnt":0,"new":0},"1":{"cnt":1,"new":1}}})";

static const string MOVE_MAILS_MESSAGE = R"(
{
   "payload" : {
      "uname" : "456",
      "fid" : "47",
      "uid" : "123",
      "mids_str" : "[\"111\",\"222\"]",
      "lcn" : "789",
      "sessionKey" : "",
      "operation" : "move mails",
      "envelopes" : [
         {
            "subject" : "Hi Jack",
            "srcFid" : 1,
            "threadId" : "111",
            "tab" : "default",
            "fid" : "47",
            "size" : 2087,
            "labels" : [
               "2170000000009092243",
               "2170000250000042541",
               "FAKE_HAS_USER_LABELS_LBL",
               "FAKE_SEEN_LBL"
            ],
            "mid" : "111",
            "labelsInfo" : {},
            "types" : [],
            "date" : 1378377600,
            "srcTab" : null,
            "from" : [
               {
                  "domain" : "domain.ru",
                  "displayName" : "Jack Daniels",
                  "local" : "user"
               }
            ],
            "messageId" : "",
            "firstline" : "test test",
            "to" : [
               {
                  "local" : "user2",
                  "displayName" : "Test User",
                  "domain" : "domain2.ru"
               }
            ],
            "fidType" : 0
         },
         {
            "date" : "",
            "types" : "",
            "labelsInfo" : {},
            "from" : "",
            "srcTab" : null,
            "labels" : [],
            "mid" : 222,
            "size" : "",
            "fid" : 37,
            "tab" : null,
            "subject" : "",
            "srcFid" : 1,
            "threadId" : "",
            "fidType" : 0,
            "to" : "",
            "firstline" : "",
            "messageId" : ""
         }
      ]
   },
   "keys" : {
      "uid" : "123",
      "lcn" : "789",
      "fid" : "47",
      "uname" : "456",
      "mids" : "[\"111\",\"222\"]",
      "method_id" : "",
      "session_key" : "",
      "operation" : "move mails"
   },
   "subscriptions" : [
      { "transport": ["http", "webpush", "websocket"] }
   ]
}
)";

static const string STATUS_CHANGE_MESSAGE = R"(
{
   "payload" : {
      "uname" : "456",
      "status" : "RO",
      "new_messages" : "",
      "sessionKey" : "",
      "uid" : "123",
      "mids_str" : "[\"333\"]",
      "operation" : "status change",
      "lcn" : "889"
   },
   "keys" : {
      "lcn" : "889",
      "operation" : "status change",
      "uid" : "123",
      "session_key" : "",
      "method_id" : "",
      "mids" : "[\"333\"]",
      "new_messages" : "",
      "status" : "RO",
      "uname" : "456"
   },
   "subscriptions" : [
      { "transport": ["http", "webpush", "websocket"] }
   ]
}
)";

static const string UPDATE_LABELS_MESSAGE = R"(
{
    "payload" : {
        "operation" : "update labels",
        "labelsDel" : [],
        "fids" : [
            "47"
        ],
        "status" : "RO",
        "uname" : "456",
        "labelsAdd" : [
            "FAKE_SEEN_LBL"
        ],
        "newCount" : "",
        "mids" : [
            "333"
        ],
        "lcn" : "889",
        "mids_str" : "[\"333\"]",
        "uid" : "123",
        "sessionKey" : "",
        "tids" : [
            "333"
        ]
    },
    "keys" : {
        "session_key" : "",
        "operation" : "update labels",
        "lcn" : "889"
    },
    "subscriptions" : [
        { "transport": ["http", "webpush", "websocket"] }
    ]
}
)";

static const string INSERT_MESSAGE = R"(
{
   "payload" : {
      "sessionKey" : "",
      "counters" : [
         5,
         7,
         6,
         3,
         1,
         1
      ],
      "uname" : "456",
      "status" : "New",
      "loc-args" : [
         "Jack Daniels",
         "Hi Jack",
         "test test"
      ],
      "tab" : "default",
      "mid" : "444",
      "envelopes" : [
         {
            "date" : 1378377600,
            "fid" : "47",
            "firstline" : "test test",
            "to" : [
               {
                  "domain" : "domain2.ru",
                  "local" : "user2",
                  "displayName" : "Test User"
               }
            ],
            "avatarUrl" : "avatar_url",
            "tab" : "default",
            "labelsInfo" : {},
            "mid" : "444",
            "threadId" : "111",
            "size" : 2087,
            "types" : [],
            "fidType" : 0,
            "messageId" : "<smth>",
            "subject" : "Hi Jack",
            "labels" : [
               "2170000000009092243",
               "2170000250000042541",
               "FAKE_HAS_USER_LABELS_LBL",
               "FAKE_SEEN_LBL"
            ],
            "from" : [
               {
                  "displayName" : "Jack Daniels",
                  "domain" : "domain.ru",
                  "local" : "user"
               }
            ]
         }
      ],
      "threadId" : "111",
      "mids_str" : "[\"444\"]",
      "uid" : "123",
      "operation" : "insert",
      "fid" : "47",
      "freshCount" : "",
      "newCount" : "",
      "lcn" : "999",
      "avatarUrl" : "avatar_url",
      "countersNew" : [
         5,
         3,
         1,
         1
      ]
   },
   "keys" : {
      "received_date" : "05.09.2013 14:40:00",
      "session_key" : "",
      "hdr_status" : "New",
      "new_messages" : "",
      "hdr_from" : "\"Jack Daniels\" <user@domain.ru>",
      "lcn" : "999",
      "fid_type" : "0",
      "sz" : "2087",
      "firstline" : "test test",
      "hdr_message_id" : "<smth>",
      "operation" : "insert",
      "hdr_subject" : "Hi Jack",
      "uname" : "456",
      "thread_id" : "111",
      "fresh_count" : "",
      "mid" : "444",
      "lid" : "2170000000009092243,2170000250000042541,FAKE_HAS_USER_LABELS_LBL,FAKE_SEEN_LBL",
      "tab" : "default",
      "uid" : "123",
      "method_id" : "",
      "fid" : "47",
      "hdr_to" : "\"Test User\" <user2@domain2.ru>"
   },
   "subscriptions" : [
      { "transport": ["http", "webpush", "websocket"] }
   ]
}
)";

inline auto make_mobile(const string& message)
{
    auto message_json = json_parse(message);
    message_json["subscriptions"][0UL]["transport"] = json_value{};
    message_json["subscriptions"][0UL]["transport"].push_back("mobile");
    return json_write(message_json);
}

static const string INSERT_PRE_RTEC_3674_MOBILE_MESSAGE = make_mobile(INSERT_MESSAGE);

static const string INSERT_NO_AVATAR_MESSAGE = R"(
{
   "keys" : {
      "tab" : "default",
      "session_key" : "",
      "uname" : "456",
      "method_id" : "",
      "thread_id" : "111",
      "mid" : "444",
      "fid_type" : "0",
      "new_messages" : "",
      "operation" : "insert",
      "hdr_to" : "\"Test User\" <user2@domain2.ru>",
      "hdr_from" : "\"Jack Daniels\" <user@domain.ru>",
      "hdr_status" : "New",
      "firstline" : "test test",
      "hdr_message_id" : "<smth>",
      "fresh_count" : "",
      "sz" : "2087",
      "fid" : "47",
      "received_date" : "05.09.2013 14:40:00",
      "lid" : "2170000000009092243,2170000250000042541,FAKE_HAS_USER_LABELS_LBL,FAKE_SEEN_LBL",
      "hdr_subject" : "Hi Jack",
      "lcn" : "999",
      "uid" : "123"
   },
   "payload" : {
      "loc-args" : [
         "Jack Daniels",
         "Hi Jack",
         "test test"
      ],
      "tab" : "default",
      "threadId" : "111",
      "mids_str" : "[\"444\"]",
      "envelopes" : [
         {
            "fid" : "47",
            "size" : 2087,
            "subject" : "Hi Jack",
            "firstline" : "test test",
            "tab" : "default",
            "threadId" : "111",
            "labels" : [
               "2170000000009092243",
               "2170000250000042541",
               "FAKE_HAS_USER_LABELS_LBL",
               "FAKE_SEEN_LBL"
            ],
            "from" : [
               {
                  "displayName" : "Jack Daniels",
                  "local" : "user",
                  "domain" : "domain.ru"
               }
            ],
            "types" : [],
            "fidType" : 0,
            "mid" : "444",
            "to" : [
               {
                  "displayName" : "Test User",
                  "local" : "user2",
                  "domain" : "domain2.ru"
               }
            ],
            "labelsInfo" : {},
            "messageId" : "<smth>",
            "date" : 1378377600
         }
      ],
      "fid" : "47",
      "uname" : "456",
      "sessionKey" : "",
      "status" : "New",
      "newCount" : "",
      "mid" : "444",
      "freshCount" : "",
      "uid" : "123",
      "operation" : "insert",
      "lcn" : "999"
   },
   "subscriptions" : [
      { "transport": ["http", "webpush", "websocket"] }
   ]
}
)";

static const string JSON_TASK_WITH_SENT = R"(
{
    "processed" : 1,
    "events" : [
        {
            "status" : {
                "avatar_fetched" : false,
                "metadata_count" : 0,
                "sent_notification_ids" : ["status change http webpush websocket"]
            },
            "items" : []
        },
        {
            "items" : [
                {
                "seen" : false,
                "to" : [
                    {
                        "local" : "user2",
                        "displayName" : "Test User",
                        "domain" : "domain2.ru"
                    }
                ],
                "size" : 2087,
                "recent" : true,
                "date" : 1378377600,
                "threadId" : "111",
                "fid" : "47",
                "specialLabels" : [],
                "deleted" : false,
                "types" : [],
                "subject" : "Hi Jack",
                "tid" : 444,
                "labels" : [
                    "2170000000009092243",
                    "2170000250000042541",
                    "FAKE_HAS_USER_LABELS_LBL",
                    "FAKE_SEEN_LBL"
                ],
                "firstline" : "test test",
                "receiveDate" : 0,
                "attachmentsCount" : 0,
                "mid" : "444",
                "tab" : "default",
                "hdrStatus" : "RO",
                "hdr_message_id" : "<smth>",
                "from" : [
                    {
                        "local" : "user",
                        "displayName" : "Jack Daniels",
                        "domain" : "domain.ru"
                    }
                ],
                "revision" : 11,
                "lids" : [
                    12
                ],
                "newCount" : 0,
                "attachmentsFullSize" : 0,
                "attachments" : []
                }
            ],
            "status" : {
                "metadata_count" : 1,
                "avatar_fetched" : false
            }
        }
    ]
}
)";

static const string JSON_TASK_AFTER_METADATA_ERROR = R"({
    "events" : [
        {
            "status" : {
                "metadata_count" : 0,
                "avatar_fetched" : false
            },
            "items" : []
        },
        {
            "status" : {
                "avatar_fetched" : false,
                "metadata_count" : 0
            },
            "items" : []
        },
        {
            "items" : [],
            "status" : {
                "avatar_fetched" : false,
                "metadata_count" : 0
            }
        }
    ],
    "processed" : 0
})";

static const string JSON_TASK_AFTER_NOTIFY_ERROR = R"({
    "processed" : 2,
    "events" : [
        {
            "status" : {
                "avatar_fetched" : true,
                "metadata_count" : 1
            },
            "items" : [
                {
                "recent" : true,
                "deleted" : false,
                "types" : [],
                "hdrStatus" : "RO",
                "from" : [
                    {
                        "local" : "user",
                        "domain" : "domain.ru",
                        "displayName" : "Jack Daniels"
                    }
                ],
                "attachmentsFullSize" : 0,
                "date" : 1378377600,
                "seen" : false,
                "tid" : 444,
                "subject" : "Hi Jack",
                "revision" : 11,
                "size" : 2087,
                "attachmentsCount" : 0,
                "lids" : [
                    12
                ],
                "labels" : [
                    "2170000000009092243",
                    "2170000250000042541",
                    "FAKE_HAS_USER_LABELS_LBL",
                    "FAKE_SEEN_LBL"
                ],
                "hdr_message_id" : "<smth>",
                "mid" : "444",
                "specialLabels" : [],
                "receiveDate" : 0,
                "avatarUrl" : "avatar_url",
                "threadId" : "111",
                "to" : [
                    {
                        "local" : "user2",
                        "domain" : "domain2.ru",
                        "displayName" : "Test User"
                    }
                ],
                "fid" : "47",
                "attachments" : [],
                "newCount" : 0,
                "firstline" : "test test",
                "tab" : "default"
                }
            ]
        }
    ]
})";

static const string JSON_TASK_AFTER_UPDATE_LABELS_FAIL = R"({
    "uid": "123",
    "suid": "456",
    "events": [{
        "change_id": 126,
        "action": "status",
        "items": [
            {"fid": 47, "mid": 333, "tab": null, "srcTab": null, "tid": 333, "lids": [9, 10], "seen": true, "recent": true, "deleted": false}
        ],
        "args": {"seen": true, "recent": null, "deleted": null, "lids_add": [], "lids_del": []},
        "ts": 124,
        "lcn": "889",
        "status": {
            "avatar_fetched": false,
            "metadata_count": 0,
            "sent_notification_ids": ["status change http webpush websocket"]
        }
    }, {
        "change_id": 127,
        "action": "store",
        "items": [
            {"deleted":false,"fid":37,"hdr_message_id":"<smth>","lids":[12],"mid":444,"recent":true,"seen":false,"tab":null,"tid":444}
        ],
        "args": {},
        "ts": 125,
        "lcn": "999"
    }]
})";

static const string SUB_LIST_WITH_MOBILE = R"([{
  "client": "test_client",
  "filter": "",
  "id": "870a4d3070ff6319119946ef3e25049953fc2c87",
  "session": "session_abcde",
  "ttl": 8,
  "url": "http://sample/callback"
},{
    "client" : "mobile",
    "device" : "test_device",
    "extra" : "",
    "filter" : "",
    "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
    "platform" : "fcm",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid"
}])";

static const string JSON_TASK_STATUS_CHANGE = R"({
    "uid": "123",
    "suid": "456",
    "events": [{
        "change_id": 126,
        "action": "status",
        "items": [
            {"fid": 47, "mid": 333, "tab": null, "srcTab": null, "tid": 333, "lids": [9, 10], "seen": true, "recent": true, "deleted": false}
        ],
        "args": {"seen": true, "recent": null, "deleted": null, "lids_add": [], "lids_del": []},
        "ts": 124,
        "lcn": "889"
    }]
})";

static string STATUS_CHANGE_ANDROID_MESSAGE = R"(
{
  "keys": {
    "lcn": "889",
    "method_id": "",
    "mids": "[\"333\"]",
    "new_messages": "",
    "operation": "status change",
    "session_key": "",
    "status": "RO",
    "uid": "123",
    "uname": "456"
  },
  "payload": {
    "lcn": "889",
    "mids": "[\"333\"]",
    "operation": "status change",
    "status": "RO",
    "uid": "123",
    "uname": "456"
  },
  "subscriptions": [
    {
      "transport": [
        "mobile"
      ]
    },
    {
      "platform": [
        "fcm", "hms"
      ]
    }
  ],
  "repack": {
     "fcm": {
        "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
     },
     "hms": {
        "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
     }
   }
}
)";

static const string INSERT_ANDROID_MESSAGE = R"(
{
   "payload" : {
      "uname" : "456",
      "status" : "New",
      "tab" : "default",
      "tid" : "111",
      "mids" : "[\"444\"]",
      "uid" : "123",
      "operation" : "insert",
      "new_messages" : "",
      "fid" : "47",
      "lcn" : "999",
      "counters" : [
         5,
         3,
         1,
         1
      ]
   },
   "keys" : {
      "received_date" : "05.09.2013 14:40:00",
      "session_key" : "",
      "hdr_status" : "New",
      "new_messages" : "",
      "hdr_from" : "\"Jack Daniels\" <user@domain.ru>",
      "lcn" : "999",
      "fid_type" : "0",
      "sz" : "2087",
      "firstline" : "test test",
      "hdr_message_id" : "<smth>",
      "operation" : "insert",
      "hdr_subject" : "Hi Jack",
      "uname" : "456",
      "thread_id" : "111",
      "fresh_count" : "",
      "mid" : "444",
      "lid" : "2170000000009092243,2170000250000042541,FAKE_HAS_USER_LABELS_LBL,FAKE_SEEN_LBL",
      "tab" : "default",
      "uid" : "123",
      "method_id" : "",
      "fid" : "47",
      "hdr_to" : "\"Test User\" <user2@domain2.ru>"
   },
   "subscriptions" : [
      { "transport": ["mobile"] },
      { "platform": ["fcm", "hms"] }
   ],
  "repack": {
     "fcm": {
        "priority": "high",
        "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
     },
     "hms": {
        "priority": "high",
        "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
     }
   }
}
)";

static const string JSON_TASK_INSERT = R"({
    "uid": "123",
    "suid": "456",
    "events": [{
        "change_id": 127,
        "action": "store",
        "items": [
            {"deleted":false,"fid":37,"hdr_message_id":"<smth>","lids":[12],"mid":444,"recent":true,"seen":false,"tab":null,"tid":444}
        ],
        "args": {},
        "ts": 125,
        "lcn": "999"
    }]
})";

static const string SUB_LIST_GCM = R"([{
    "client" : "mobile",
    "device" : "test_device",
    "extra" : "",
    "filter" : "",
    "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
    "platform" : "gcm",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid"
}])";

static const string SUB_LIST_APNS = R"([{
    "client" : "ru_yandex_mail_30821_605__iPhone9_3__iOS_12_2_",
    "device" : "test_device",
    "extra" : "{\"sound\": \"p.caf\"}",
    "filter" : "{\"rules\":[{\"do\":\"send_silent\",\"if\":\"EXFID\"}], \"vars\":{\"EXFID\":{\"fid\":{\"$eq\":[\"12\"]}}}}",
    "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
    "platform" : "apns",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid"
}])";

static const string INSERT_APNS_BRIGHT_MESSAGE = R"(
{
   "payload" : {
      "u" : "456",
      "tab" : "default",
      "m" : "444",
      "tid" : "111",
      "z" : "123",
      "operation" : "insert",
      "fid" : "47",
      "new_messages" : "",
      "lcn" : "999",
      "avatar" : "avatar_url",
      "counters" : [
         5,
         3,
         1,
         1
      ]
   },
   "keys" : {
      "received_date" : "05.09.2013 14:40:00",
      "session_key" : "",
      "hdr_status" : "New",
      "new_messages" : "",
      "hdr_from" : "\"Jack Daniels\" <user@domain.ru>",
      "lcn" : "999",
      "fid_type" : "0",
      "sz" : "2087",
      "firstline" : "test test",
      "hdr_message_id" : "<smth>",
      "operation" : "insert",
      "hdr_subject" : "Hi Jack",
      "uname" : "456",
      "thread_id" : "111",
      "fresh_count" : "",
      "mid" : "444",
      "lid" : "2170000000009092243,2170000250000042541,FAKE_HAS_USER_LABELS_LBL,FAKE_SEEN_LBL",
      "tab" : "default",
      "uid" : "123",
      "method_id" : "",
      "fid" : "47",
      "hdr_to" : "\"Test User\" <user2@domain2.ru>"
   },
   "subscriptions" : [
      { "transport": ["mobile"] },
      { "platform": ["apns"] },
      { "subscription_id": ["mob:870a4d3070ff6319119946ef3e25049953fc2c88"] }
   ],
   "repack": {
      "apns": {
        "collapse-id": "123_999",
        "aps": {
          "alert": {
            "title": "Jack Daniels",
            "subtitle": "Hi Jack",
            "body": "test test"
          },
          "category": "M",
          "sound": "p.caf",
          "mutable-content": 1
        },
        "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
      }
   }
}
)";

static const string INSERT_APNS_SILENT_MESSAGE = R"(
{
   "payload" : {
      "u" : "456",
      "tab" : "default",
      "m" : "444",
      "tid" : "111",
      "z" : "123",
      "operation" : "insert",
      "fid" : "47",
      "new_messages" : "",
      "lcn" : "999",
      "avatar" : "avatar_url",
      "counters" : [
         5,
         3,
         1,
         1
      ]
   },
   "keys" : {
      "received_date" : "05.09.2013 14:40:00",
      "session_key" : "",
      "hdr_status" : "New",
      "new_messages" : "",
      "hdr_from" : "\"Jack Daniels\" <user@domain.ru>",
      "lcn" : "999",
      "fid_type" : "0",
      "sz" : "2087",
      "firstline" : "test test",
      "hdr_message_id" : "<smth>",
      "operation" : "insert",
      "hdr_subject" : "Hi Jack",
      "uname" : "456",
      "thread_id" : "111",
      "fresh_count" : "",
      "mid" : "444",
      "lid" : "2170000000009092243,2170000250000042541,FAKE_HAS_USER_LABELS_LBL,FAKE_SEEN_LBL",
      "tab" : "default",
      "uid" : "123",
      "method_id" : "",
      "fid" : "47",
      "hdr_to" : "\"Test User\" <user2@domain2.ru>"
   },
   "subscriptions" : [
      { "transport": ["mobile"] },
      { "platform": ["apns"] }
   ],
   "repack": {
      "apns": {
        "aps": {
          "content-available": 1,
          "sound": ""
        },
        "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
      }
   }
}
)";

static const string UPDATE_LABELS_APNS_MESSAGE = R"(
{
    "payload" : {
        "operation" : "update labels",
        "fids" : [
            "47"
        ],
        "u" : "456",
        "new_messages" : "",
        "mids" : [
            "333"
        ],
        "tabs": [
            ""
        ],
        "lcn" : "889",
        "z" : "123"
    },
    "keys" : {
        "session_key" : "",
        "operation" : "update labels",
        "lcn" : "889"
    },
    "subscriptions" : [
       { "transport": ["mobile"] },
       { "platform": ["apns"] }
    ],
    "repack": {
        "apns": {
            "aps": {
                "content-available": 1,
                "sound": ""
            },
            "repack_payload": ["*", {"transit-id": "::xiva::transit_id"}]
        }
    }
}
)";

static const string SUB_LIST_APNS_MULTIACCOUNT_123 = R"([{
    "client" : "ru_yandex_mail_30821_605__iPhone9_3__iOS_12_2_",
    "device" : "test_device",
    "extra" : "{\"sound\": \"p.caf\", \"accounts\": [{ \"uid\": \"123\", \"environment\": \"production\", \"badge\": { \"enabled\": true } },{ \"uid\": \"321\", \"environment\": \"production\", \"badge\": { \"enabled\": true } }]}",
    "filter" : "{\"rules\":[{\"do\":\"send_silent\",\"if\":\"EXFID\"}], \"vars\":{\"EXFID\":{\"fid\":{\"$eq\":[\"12\"]}}}}",
    "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
    "platform" : "apns",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid1"
}])";

static const string SUB_LIST_APNS_MULTIACCOUNT_321 = R"([{
    "client" : "ru_yandex_mail_30821_605__iPhone9_3__iOS_12_2_",
    "device" : "test_device",
    "extra" : "{\"sound\": \"p.caf\", \"accounts\": [{ \"uid\": \"321\", \"environment\": \"production\", \"badge\": { \"enabled\": true } },{ \"uid\": \"123\", \"environment\": \"production\", \"badge\": { \"enabled\": true } }]}",
    "filter" : "{\"rules\":[{\"do\":\"send_silent\",\"if\":\"EXFID\"}], \"vars\":{\"EXFID\":{\"fid\":{\"$eq\":[\"12\"]}}}}",
    "id" : "mob:2",
    "platform" : "apns",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid2"
}])";

static const string SUB_LIST_APNS_CROSS_ENVIRONMENT_PROD = R"([{
    "client" : "ru_yandex_mail_30821_605__iPhone9_3__iOS_12_2_",
    "device" : "test_device",
    "extra" : "{\"sound\": \"p.caf\", \"accounts\": [{ \"uid\": \"prod\", \"environment\": \"production\", \"badge\": { \"enabled\": true } },{ \"uid\": \"corp\", \"environment\": \"corp\", \"badge\": { \"enabled\": true } }]}",
    "filter" : "{\"rules\":[{\"do\":\"send_silent\",\"if\":\"EXFID\"}], \"vars\":{\"EXFID\":{\"fid\":{\"$eq\":[\"12\"]}}}}",
    "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
    "platform" : "apns",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid1"
}])";

static const string SUB_LIST_APNS_CROSS_ENVIRONMENT_CORP = R"([{
    "client" : "ru_yandex_mail_30821_605__iPhone9_3__iOS_12_2_",
    "device" : "test_device",
    "extra" : "{\"sound\": \"p.caf\", \"accounts\": [{ \"uid\": \"corp\", \"environment\": \"corp\", \"badge\": { \"enabled\": true } },{ \"uid\": \"prod\", \"environment\": \"production\", \"badge\": { \"enabled\": true } }]}",
    "filter" : "{\"rules\":[{\"do\":\"send_silent\",\"if\":\"EXFID\"}], \"vars\":{\"EXFID\":{\"fid\":{\"$eq\":[\"12\"]}}}}",
    "id" : "mob:2",
    "platform" : "apns",
    "app": "ru.yandex.mail",
    "session" : "test_session",
    "ttl" : 31536000,
    "uuid" : "test_uuid2"
}])";

static const string COUNTERS_123 = R"({"folders":{"1":{"cnt":1,"new":1}}})";
static const string COUNTERS_321 = R"({"folders":{"1":{"cnt":5,"new":2}}})";

static const string COUNTERS_PROD = R"({"folders":{"1":{"cnt":1,"new":1}}})";
static const string COUNTERS_CORP = R"({"folders":{"1":{"cnt":5,"new":2}}})";

inline auto add_badge(unsigned badge, const string& message)
{
    json_value message_json = json_parse(message);
    message_json["repack"]["apns"]["aps"]["badge"] = badge;
    return json_write(message_json);
}

inline auto rewrite_counters(std::vector<unsigned> new_counters, const string& message)
{
    json_value message_json = json_parse(message);
    message_json["payload"]["counters"] = json_value(json_type::tarray);
    for (auto& counter : new_counters)
    {
        message_json["payload"]["counters"].push_back(counter);
    }
    return json_write(message_json);
}

static const string INSERT_APNS_BRIGHT_MESSAGE_WITH_BADGE =
    rewrite_counters({ 1, 1 }, add_badge(3, INSERT_APNS_BRIGHT_MESSAGE));
