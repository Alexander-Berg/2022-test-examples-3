{
  "@class" : ".MpfsRequestRecord$ExtractArchive",
  "meta" : {
    "id" : {
      "id" : "20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2"
    },
    "created" : "2012-10-16T09:17:08+00:00"
  },
  "revision" : {
    "n" : 64,
    "updated" : {
      "host" : "alexm-nb",
      "instant" : "2012-10-16T09:17:09+00:00"
    }
  },
  "lease" : [ {
    "host" : "alexm-nb",
    "whenAssigned" : "2012-10-16T09:17:08+00:00",
    "alive" : [ "2012-10-16T09:17:09+00:00" ]
  } ],
  "request" : {
    "@class" : "ru.yandex.chemodan.uploader.registry.record.MpfsRequest$ExtractArchive",
    "apiVersion" : {
      "string" : "0.2"
    },
    "originalFile" : {
      "stid" : "1000005.tmp.426298960293423098715012358255",
      "part" : ""
    },
    "fileToExtract" : [ ],
    "chemodanFile" : {
      "uidOrSpecial" : {
        "@class" : ".UidOrSpecial$Uid",
        "passportUid" : 16011578
      },
      "uniqueFileId" : "123",
      "path" : "/foo"
    },
    "callbackUri" : [ ],
    "maxFileSize" : [ ]
  },
  "status" : {
    "@class" : "ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus$ExtractArchive",
    "originalFile" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-10-16T09:17:08+00:00",
        "end" : "2012-10-16T09:17:08+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "localFile" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/download-2",
        "mulcaId" : {
          "stid" : "1000005.tmp.426298960293423098715012358255",
          "part" : ""
        }
      }
    },
    "extractedFiles" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-10-16T09:17:08+00:00",
        "end" : "2012-10-16T09:17:08+00:00"
      },
      "lastProgress" : [ ],
      "result" : [ {
        "entry" : {
          "path" : "a/b/lala.txt",
          "readablePath" : "a/b/lala.txt",
          "size" : 5,
          "isFolder" : false,
          "isPassworded" : false
        },
        "localFile" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/extracted-3"
      }, {
        "entry" : {
          "path" : "a/lala.txt",
          "readablePath" : "a/lala.txt",
          "size" : 5,
          "isFolder" : false,
          "isPassworded" : false
        },
        "localFile" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/extracted-4"
      }, {
        "entry" : {
          "path" : "lala.txt",
          "readablePath" : "lala.txt",
          "size" : 5,
          "isFolder" : false,
          "isPassworded" : false
        },
        "localFile" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/extracted-5"
      } ]
    },
    "multipleUpload" : {
      "queue" : [ {
        "isActive" : false,
        "userFile" : {
          "contentType" : [ ],
          "contentLength" : [ 5 ],
          "file" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/extracted-3"
        },
        "payloadInfo" : {
          "@class" : ".State$Success",
          "failedAttempts" : 0,
          "executionInterval" : {
            "start" : "2012-10-16T09:17:08+00:00",
            "end" : "2012-10-16T09:17:08+00:00"
          },
          "lastProgress" : [ ],
          "result" : {
            "contentType" : [ "text/plain" ],
            "contentLength" : 5,
            "md5" : "549e80f319af070f8ea8d0f149a149c2",
            "sha256" : "2dab7013f332b465b23e912d90d84c166aefbf60689242166e399d7add1c0189"
          }
        },
        "postProcess" : {
          "commitFileInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:08+00:00",
              "end" : "2012-10-16T09:17:08+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "callbackResponse" : [ ]
            }
          },
          "fileMulcaUploadInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:08+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "mulcaId" : {
                "stid" : "1000001.tmp.426298960270263952510479298668",
                "part" : ""
              },
              "size" : [ 5 ]
            }
          },
          "digestMulcaUploadInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "mulcaId" : {
                "stid" : "1000001.tmp.426298960256026673212869003372",
                "part" : ""
              },
              "size" : [ 37 ]
            }
          },
          "commitFileUpload" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "callbackResponse" : [ ]
            }
          },
          "previewImageStatus" : {
          },
          "antivirusResult2" : {
            "@class" : ".State$Disabled",
            "failedAttempts" : 0
          }
        },
        "index" : 0,
        "chemodanFile" : {
          "uidOrSpecial" : {
            "@class" : ".UidOrSpecial$Uid",
            "passportUid" : 16011578
          },
          "uniqueFileId" : "",
          "path" : "a/b/lala.txt"
        }
      }, {
        "isActive" : false,
        "userFile" : {
          "contentType" : [ ],
          "contentLength" : [ 5 ],
          "file" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/extracted-4"
        },
        "payloadInfo" : {
          "@class" : ".State$Success",
          "failedAttempts" : 0,
          "executionInterval" : {
            "start" : "2012-10-16T09:17:09+00:00",
            "end" : "2012-10-16T09:17:09+00:00"
          },
          "lastProgress" : [ ],
          "result" : {
            "contentType" : [ "text/plain" ],
            "contentLength" : 5,
            "md5" : "549e80f319af070f8ea8d0f149a149c2",
            "sha256" : "2dab7013f332b465b23e912d90d84c166aefbf60689242166e399d7add1c0189"
          }
        },
        "postProcess" : {
          "commitFileInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "callbackResponse" : [ ]
            }
          },
          "fileMulcaUploadInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "mulcaId" : {
                "stid" : "1000008.tmp.4262989602139305226128191778924",
                "part" : ""
              },
              "size" : [ 5 ]
            }
          },
          "digestMulcaUploadInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "mulcaId" : {
                "stid" : "1000001.tmp.4262989602123258242331784331372",
                "part" : ""
              },
              "size" : [ 37 ]
            }
          },
          "commitFileUpload" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "callbackResponse" : [ ]
            }
          },
          "previewImageStatus" : {
          },
          "antivirusResult2" : {
            "@class" : ".State$Disabled",
            "failedAttempts" : 0
          }
        },
        "index" : 1,
        "chemodanFile" : {
          "uidOrSpecial" : {
            "@class" : ".UidOrSpecial$Uid",
            "passportUid" : 16011578
          },
          "uniqueFileId" : "",
          "path" : "a/lala.txt"
        }
      }, {
        "isActive" : false,
        "userFile" : {
          "contentType" : [ ],
          "contentLength" : [ 5 ],
          "file" : "/tmp/localfiles-TESTS/20121016T091708.792.ear.1c2i6swex3ntw8pyrkdn2f7ri-alexm-nb.alexm-nb-2/extracted-5"
        },
        "payloadInfo" : {
          "@class" : ".State$Success",
          "failedAttempts" : 0,
          "executionInterval" : {
            "start" : "2012-10-16T09:17:09+00:00",
            "end" : "2012-10-16T09:17:09+00:00"
          },
          "lastProgress" : [ ],
          "result" : {
            "contentType" : [ "text/plain" ],
            "contentLength" : 5,
            "md5" : "549e80f319af070f8ea8d0f149a149c2",
            "sha256" : "2dab7013f332b465b23e912d90d84c166aefbf60689242166e399d7add1c0189"
          }
        },
        "postProcess" : {
          "commitFileInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "callbackResponse" : [ ]
            }
          },
          "fileMulcaUploadInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "mulcaId" : {
                "stid" : "1000002.tmp.4262989602204307041543936147564",
                "part" : ""
              },
              "size" : [ 5 ]
            }
          },
          "digestMulcaUploadInfo" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "mulcaId" : {
                "stid" : "1000008.tmp.4262989602175190990147845763180",
                "part" : ""
              },
              "size" : [ 37 ]
            }
          },
          "commitFileUpload" : {
            "@class" : ".State$Success",
            "failedAttempts" : 0,
            "executionInterval" : {
              "start" : "2012-10-16T09:17:09+00:00",
              "end" : "2012-10-16T09:17:09+00:00"
            },
            "lastProgress" : [ ],
            "result" : {
              "callbackResponse" : [ ]
            }
          },
          "previewImageStatus" : {
          },
          "antivirusResult2" : {
            "@class" : ".State$Disabled",
            "failedAttempts" : 0
          }
        },
        "index" : 2,
        "chemodanFile" : {
          "uidOrSpecial" : {
            "@class" : ".UidOrSpecial$Uid",
            "passportUid" : 16011578
          },
          "uniqueFileId" : "",
          "path" : "lala.txt"
        }
      } ]
    },
    "commitFinal" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-10-16T09:17:09+00:00",
        "end" : "2012-10-16T09:17:09+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "callbackResponse" : [ ]
      }
    },
    "notCancelledByAdmin" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-10-16T09:17:08+00:00",
        "end" : "2012-10-16T09:17:08+00:00"
      },
      "lastProgress" : [ ],
      "result" : true
    },
    "internalError" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-10-16T09:17:08+00:00",
        "end" : "2012-10-16T09:17:08+00:00"
      },
      "lastProgress" : [ ],
      "result" : false
    }
  }
}
