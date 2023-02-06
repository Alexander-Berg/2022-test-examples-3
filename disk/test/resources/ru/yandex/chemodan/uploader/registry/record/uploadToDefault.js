{
  "@class" : ".MpfsRequestRecord$UploadToDefault",
  "meta" : {
    "id" : {
      "id" : "20120727T115044.591.utd.5aseyvqgspsxiqjyarhyrpifi-k.k-3874"
    },
    "created" : "2012-07-27T11:50:44+00:00"
  },
  "revision" : {
    "n" : 27,
    "updated" : {
      "host" : "uploader-tst.disk.yandex.net",
      "instant" : "2012-07-27T11:50:46+00:00"
    }
  },
  "lease" : [ {
    "host" : "uploader-tst.disk.yandex.net",
    "whenAssigned" : "2012-07-27T11:50:44+00:00",
    "alive" : [ "2012-07-27T11:50:46+00:00" ]
  } ],
  "request" : {
    "@class" : ".MpfsRequest$UploadToDefault",
    "apiVersion" : {
      "string" : "0.2"
    },
    "chemodanFile" : {
      "uidOrSpecial" : {
        "@class" : ".UidOrSpecial$Uid",
        "passportUid" : 7777777
      },
      "uniqueFileId" : "2cdfd2f2c795ccee87bfc85f150547cafd9a9d14f88f7e284294ab3d22723fa7",
      "path" : "7777777:/attach/PC282494.jpg"
    },
    "callbackUri" : [ "http://v3.virt.mail.yandex.net/service/kladun_callback?uid=7777777&oid=3cd6fe9a9d7340b446ba64a9d863355a8e1723af659ebd54ffd7648e89c4b185" ],
    "maxFileSize" : [ 107374182400 ],
    "yandexCloudRequestId": [ "asd123LKJ" ]
  },
  "status" : {
    "@class" : ".MpfsRequestStatus$UploadToDefault",
    "userFile" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T11:50:44+00:00",
        "end" : "2012-07-27T11:50:44+00:00"
      },
      "lastProgress" : [ {
        "processed" : 124244,
        "total" : [ 124244 ]
      } ],
      "result" : {
        "contentType" : [ ],
        "contentLength" : [ 124244 ],
        "file" : "/var/spool/yandex/chemodan/uploader/local-files/20120727T115044.591.utd.5aseyvqgspsxiqjyarhyrpifi-k.k-3874/incoming-2464"
      }
    },
    "payloadInfo" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T11:50:45+00:00",
        "end" : "2012-07-27T11:50:45+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "contentType" : [ "image/jpeg" ],
        "contentLength" : 124244,
        "md5" : "2c13a3ef07417e63ebe849133ca68b28",
        "sha256" : "f7ea75269c676b7337641b970ac53f88c9736c67fb83096d0e32b27e3ef173ae"
      }
    },
    "postProcess" : {
      "commitFileInfo" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T11:50:45+00:00",
          "end" : "2012-07-27T11:50:45+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "callbackResponse" : [ {
            "statusCode" : 200,
            "statusLine" : "HTTP/1.1 200 OK",
            "response" : "null",
            "uri" : [ "http://v3.virt.mail.yandex.net/service/kladun_callback?uid=7777777&oid=3cd6fe9a9d7340b446ba64a9d863355a8e1723af659ebd54ffd7648e89c4b185" ]
          } ]
        }
      },
      "fileMulcaUploadInfo" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T11:50:45+00:00",
          "end" : "2012-07-27T11:50:45+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "mulcaId" : {
            "stid" : "1000008.yadisk:7777777.39832963846850353664811234290",
            "part" : ""
          },
          "size" : [ 124244 ]
        }
      },
      "digestMulcaUploadInfo" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T11:50:45+00:00",
          "end" : "2012-07-27T11:50:45+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "mulcaId" : {
            "stid" : "1000003.yadisk:7777777.3983296384464395048266226674",
            "part" : ""
          },
          "size" : [ 1278 ]
        }
      },
      "commitFileUpload" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T11:50:45+00:00",
          "end" : "2012-07-27T11:50:45+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "callbackResponse" : [ {
            "statusCode" : 200,
            "statusLine" : "HTTP/1.1 200 OK",
            "response" : "null",
            "uri" : [ "http://v3.virt.mail.yandex.net/service/kladun_callback?uid=7777777&oid=3cd6fe9a9d7340b446ba64a9d863355a8e1723af659ebd54ffd7648e89c4b185" ]
          } ]
        }
      },
      "previewImageStatus" : {
      },
      "antivirusResult2" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T11:50:46+00:00",
          "end" : "2012-07-27T11:50:46+00:00"
        },
        "lastProgress" : [ ],
        "result" : "HEALTHY"
      }
    },
    "commitFinal" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T11:50:46+00:00",
        "end" : "2012-07-27T11:50:46+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "callbackResponse" : [ {
          "statusCode" : 200,
          "statusLine" : "HTTP/1.1 200 OK",
          "response" : "null",
          "uri" : [ "http://v3.virt.mail.yandex.net/service/kladun_callback?uid=7777777&oid=3cd6fe9a9d7340b446ba64a9d863355a8e1723af659ebd54ffd7648e89c4b185" ]
        } ]
      }
    },
    "notCancelledByAdmin" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T11:50:44+00:00",
        "end" : "2012-07-27T11:50:44+00:00"
      },
      "lastProgress" : [ ],
      "result" : true
    },
    "internalError" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T11:50:44+00:00",
        "end" : "2012-07-27T11:50:44+00:00"
      },
      "lastProgress" : [ ],
      "result" : false
    }
  }
}
