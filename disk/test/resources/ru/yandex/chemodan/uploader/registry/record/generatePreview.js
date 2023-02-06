{
  "@class" : ".MpfsRequestRecord$GeneratePreview",
  "meta" : {
    "id" : {
      "id" : "20130814T101105.329.gip.2x5io6o81hd5uwdeq88eflrvt-ssytnik-ubuntu.ssytnik-ubuntu-2"
    },
    "created" : "2013-08-14T10:11:05+00:00"
  },
  "revision" : {
    "n" : 4,
    "updated" : {
      "host" : "ssytnik-ubuntu",
      "instant" : "2013-08-14T10:11:05+00:00"
    }
  },
  "lease" : [ {
    "host" : "ssytnik-ubuntu",
    "whenAssigned" : "2013-08-14T10:11:05+00:00",
    "alive" : [ "2013-08-14T10:11:05+00:00" ]
  } ],
  "request" : {
    "@class" : "ru.yandex.chemodan.uploader.registry.record.MpfsRequest$GeneratePreview",
    "apiVersion" : {
      "string" : "0.2"
    },
    "originalFile" : {
      "stid" : "1000008.tmp.426298960262407479719460881136",
      "part" : ""
    },
    "size" : {
      "size" : [ ],
      "dimension" : [ {
        "height" : 100,
        "width" : 100
      } ]
    },
    "crop" : false,
    "logoPosition" : "SOUTH_EAST",
    "callbackUri" : [ ],
    "maxFileSize" : [ ],
    "chemodanFile" : {
      "uidOrSpecial" : {
        "@class" : ".UidOrSpecial$Uid",
        "passportUid" : 1
      },
      "uniqueFileId" : "unused",
      "path" : "unused"
    }
  },
  "status" : {
    "@class" : "ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus$GeneratePreview",
    "originalFile" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2013-08-14T10:11:05+00:00",
        "end" : "2013-08-14T10:11:05+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "localFile" : "/tmp/localfiles-TESTS/20130814T101105.329.gip.2x5io6o81hd5uwdeq88eflrvt-ssytnik-ubuntu.ssytnik-ubuntu-2/download-2",
        "mulcaId" : {
          "stid" : "1000008.tmp.426298960262407479719460881136",
          "part" : ""
        }
      }
    },
    "generatePreview" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2013-08-14T10:11:05+00:00",
        "end" : "2013-08-14T10:11:05+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "previewFile" : "/tmp/localfiles-TESTS/20130814T101105.329.gip.2x5io6o81hd5uwdeq88eflrvt-ssytnik-ubuntu.ssytnik-ubuntu-2/preview",
        "previewFormat" : "JPEG",
        "origSize" : [ {
          "height" : 300,
          "width" : 199
        } ],
        "previewSize" : {
          "height" : 100,
          "width" : 66
        }
      }
    },
    "addLogo" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2013-08-14T10:11:05+00:00",
        "end" : "2013-08-14T10:11:05+00:00"
      },
      "lastProgress" : [ ],
      "result" : true
    },
    "commitFinal" : {
      "@class" : ".State$Initial",
      "failedAttempts" : 0
    },
    "notCancelledByAdmin" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2013-08-14T10:11:05+00:00",
        "end" : "2013-08-14T10:11:05+00:00"
      },
      "lastProgress" : [ ],
      "result" : true
    },
    "internalError" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2013-08-14T10:11:05+00:00",
        "end" : "2013-08-14T10:11:05+00:00"
      },
      "lastProgress" : [ ],
      "result" : false
    }
  }
}
