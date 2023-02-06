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
    "@class" : "ru.yandex.chemodan.uploader.registry.record.MpfsRequest$UploadToDefault",
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
    "yandexCloudRequestId" : [ "asd123LKJ" ]
  },
  "status" : {
    "@class" : "ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus$UploadToDefault",
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
        "file" : "/var/spool/yandex/chemodan/uploader/local-files/20120727T115044.591.utd.5aseyvqgspsxiqjyarhyrpifi-k.k-3874/incoming-2464",
        "uploadedPartsInfo" : [ ]
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
            "headers" : { },
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
      "digestCalculationStatus" : {
        "@class" : ".State$TemporaryFailure",
        "failedAttempts" : 1,
        "executionInterval" : {
          "start" : "2017-01-27T09:23:08+00:00",
          "end" : "2017-01-27T09:23:09+00:00"
        },
        "lastProgress" : [ ],
        "retryAfter" : "2017-01-27T09:23:10+00:00",
        "failureCause" : {
          "message" : "no error",
          "stackTrace" : "",
          "details" : { }
        }
      },
      "digestMulcaUploadInfo" : {
        "@class" : ".State$TemporaryFailure",
        "failedAttempts" : 1,
        "executionInterval" : {
          "start" : "2017-01-27T09:23:09+00:00",
          "end" : "2017-01-27T09:23:09+00:00"
        },
        "lastProgress" : [ ],
        "retryAfter" : "2017-01-27T09:23:10+00:00",
        "failureCause" : {
          "message" : "no error",
          "stackTrace" : "",
          "details" : { }
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
            "headers" : { },
            "uri" : [ "http://v3.virt.mail.yandex.net/service/kladun_callback?uid=7777777&oid=3cd6fe9a9d7340b446ba64a9d863355a8e1723af659ebd54ffd7648e89c4b185" ]
          } ]
        }
      },
      "previewDocumentStatus" : {
        "generatePreview" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        },
        "previewMulcaUploadInfo" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        }
      },
      "previewImageStatus" : {
        "generateOnePreview" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        },
        "previewMulcaUploadInfo" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        }
      },
      "previewVideoStatus" : {
        "generatePreview" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        },
        "previewMulcaUploadInfo" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        },
        "multiplePreviewMulcaUploadInfo" : {
          "@class" : ".State$Initial",
          "failedAttempts" : 0
        }
      },
      "exifInfo" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      },
      "mediaInfo" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      },
      "videoInfo" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
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
          "headers" : { },
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