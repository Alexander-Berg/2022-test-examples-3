{
  "@class" : ".MpfsRequestRecord$UploadFromService",
  "meta" : {
    "id" : {
      "id" : "20120727T100019.364.ufs.eluhlyh3cvdd9dj9c0ro7vuh6-k.k-3834"
    },
    "created" : "2012-07-27T10:00:19+00:00"
  },
  "revision" : {
    "n" : 25,
    "updated" : {
      "host" : "uploader-tst.disk.yandex.net",
      "instant" : "2012-07-27T10:00:25+00:00"
    }
  },
  "lease" : [ {
    "host" : "uploader-tst.disk.yandex.net",
    "whenAssigned" : "2012-07-27T10:00:19+00:00",
    "alive" : [ "2012-07-27T10:00:25+00:00" ]
  } ],
  "request" : {
    "@class" : ".MpfsRequest$UploadFromService",
    "apiVersion" : {
      "string" : "0.2"
    },
    "sourceService" : "MAIL2",
    "serviceFileId" : [{
      "uid" : 14367847,
      "n" : "92588001.6a9628c34940ea79ddaadbca83e59ff2"
    }],
    "chemodanFile" : {
      "uidOrSpecial" : {
        "@class" : ".UidOrSpecial$Uid",
        "passportUid" : 14367847
      },
      "uniqueFileId" : "f515c6006f7cfc8aa647ba1d513335c8d5158276295fba8efda164eeb6fc1fa2",
      "path" : "14367847:/disk/progit.pdf"
    },
    "callbackUri" : [ "http://v7.virt.mail.yandex.net/service/kladun_callback?uid=14367847&oid=510da22f14668cca24f1d5fdb681c093d84530db548cd2eaa7d28ce3d239d446" ],
    "maxFileSize" : [ ]
  },
  "status" : {
    "@class" : ".MpfsRequestStatus$UploadFromService",
    "downloadedFileFromService2" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T10:00:19+00:00",
        "end" : "2012-07-27T10:00:23+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "incomingFile" : {
          "contentType" : [ ],
          "contentLength" : [ 4052928 ],
          "file" : "/var/spool/yandex/chemodan/uploader/local-files/20120727T100019.364.ufs.eluhlyh3cvdd9dj9c0ro7vuh6-k.k-3834/download-2434"
        },
        "service" : "MAIL2",
        "serviceFileId" : [{
          "uid" : 14367847,
          "n" : "92588001.6a9628c34940ea79ddaadbca83e59ff2"
        }]
      }
    },
    "downloadedFileInfo" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T10:00:23+00:00",
        "end" : "2012-07-27T10:00:23+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "contentType" : [ "application/pdf" ],
        "contentLength" : 4052928,
        "md5" : "9b7cc1bf5d3a3637e9ba3894f9f7d405",
        "sha256" : "7262c043b37b872d764320f97cf3e8cda5068a7d241fdede51fde8438704e763"
      }
    },
    "postProcess" : {
      "commitFileInfo" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T10:00:23+00:00",
          "end" : "2012-07-27T10:00:23+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "callbackResponse" : [ {
            "statusCode" : 200,
            "statusLine" : "HTTP/1.1 200 OK",
            "response" : "null",
            "uri" : [ "http://v7.virt.mail.yandex.net/service/kladun_callback?uid=14367847&oid=510da22f14668cca24f1d5fdb681c093d84530db548cd2eaa7d28ce3d239d446" ]
          } ]
        }
      },
      "fileMulcaUploadInfo" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T10:00:23+00:00",
          "end" : "2012-07-27T10:00:24+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "mulcaId" : {
            "stid" : "1000003.yadisk:14367847.3983296384128812767959018411472",
            "part" : ""
          },
          "size" : [ 4052928 ]
        }
      },
      "digestMulcaUploadInfo" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T10:00:24+00:00",
          "end" : "2012-07-27T10:00:24+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "mulcaId" : {
            "stid" : "1000006.yadisk:14367847.398329638410219334687316012511",
            "part" : ""
          },
          "size" : [ 40278 ]
        }
      },
      "commitFileUpload" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T10:00:24+00:00",
          "end" : "2012-07-27T10:00:24+00:00"
        },
        "lastProgress" : [ ],
        "result" : {
          "callbackResponse" : [ {
            "statusCode" : 200,
            "statusLine" : "HTTP/1.1 200 OK",
            "response" : "null",
            "uri" : [ "http://v7.virt.mail.yandex.net/service/kladun_callback?uid=14367847&oid=510da22f14668cca24f1d5fdb681c093d84530db548cd2eaa7d28ce3d239d446" ]
          } ]
        }
      },
      "previewImageStatus" : {
      },
      "antivirusResult2" : {
        "@class" : ".State$Success",
        "failedAttempts" : 0,
        "executionInterval" : {
          "start" : "2012-07-27T10:00:24+00:00",
          "end" : "2012-07-27T10:00:25+00:00"
        },
        "lastProgress" : [ ],
        "result" : "HEALTHY"
      }
    },
    "commitFinal" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T10:00:25+00:00",
        "end" : "2012-07-27T10:00:25+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "callbackResponse" : [ {
          "statusCode" : 200,
          "statusLine" : "HTTP/1.1 200 OK",
          "response" : "null",
          "uri" : [ "http://v7.virt.mail.yandex.net/service/kladun_callback?uid=14367847&oid=510da22f14668cca24f1d5fdb681c093d84530db548cd2eaa7d28ce3d239d446" ]
        } ]
      }
    },
    "notCancelledByAdmin" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T10:00:19+00:00",
        "end" : "2012-07-27T10:00:19+00:00"
      },
      "lastProgress" : [ ],
      "result" : true
    },
    "internalError" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-07-27T10:00:19+00:00",
        "end" : "2012-07-27T10:00:19+00:00"
      },
      "lastProgress" : [ ],
      "result" : false
    }
  }
}
