{
  "@class" : ".MpfsRequestRecord$PatchAtDefault",
  "meta" : {
    "id" : {
      "id" : "20120803T192522.617.pad.c0lfutd37qa7u9ama3bfoohme-k1h.k1h-1856"
    },
    "created" : "2012-08-03T19:25:22+00:00"
  },
  "revision" : {
    "n" : 7,
    "updated" : {
      "host" : "uploader1h.disk.yandex.net",
      "instant" : "2012-08-03T19:55:23+00:00"
    }
  },
  "lease" : [ {
    "host" : "uploader1h.disk.yandex.net",
    "whenAssigned" : "2012-08-03T19:25:22+00:00",
    "alive" : [ "2012-08-03T19:55:23+00:00" ]
  } ],
  "request" : {
    "@class" : ".MpfsRequest$PatchAtDefault",
    "apiVersion" : {
      "string" : "0.2"
    },
    "originalFile" : {
      "stid" : "4010.yadisk:98931931.2392602731210147502658087637962",
      "part" : ""
    },
    "originalMd5" : "9831549475f2dff26553da3016e428a5",
    "chemodanFile" : {
      "uidOrSpecial" : {
        "@class" : ".UidOrSpecial$Uid",
        "passportUid" : 98931931
      },
      "uniqueFileId" : "54a80f002e6651533cd2ea7b36f478738743c9d9fb90f06aa9bd40729223fd3b",
      "path" : "98931931:/disk/Документы/Текст/QIP History/InfICQ1_410555222.qhf"
    },
    "callbackUri" : [ "http://mpfs.disk.yandex.net:80/service/kladun_callback?uid=98931931&oid=092bde6c3a353dc94f4847d9046d5ee7c4afdabd1ac06265712efc11860676c5" ],
    "maxFileSize" : [ 10099196740 ]
  },
  "status" : {
    "@class" : ".MpfsRequestStatus$PatchAtDefault",
    "expectedPatchedMd5" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-08-03T19:25:22+00:00",
        "end" : "2012-08-03T19:25:22+00:00"
      },
      "lastProgress" : [ ],
      "result" : "f010dde10ca4fc058ded966ab9beeb20"
    },
    "incomingPatch" : {
      "@class" : ".State$PermanentFailure",
      "failedAttempts" : 1,
      "executionInterval" : {
        "start" : "2012-08-03T19:25:22+00:00",
        "end" : "2012-08-03T19:55:23+00:00"
      },
      "lastProgress" : [ {
        "processed" : 0,
        "total" : [ 4068 ]
      } ],
      "failureCause" : {
        "message" : "java.lang.RuntimeException: Too much time has passed since last upload activity at 2012-08-03T19:25:22.627Z",
        "stackTrace" : "java.lang.RuntimeException: Too much time has passed since last upload activity at 2012-08-03T19:25:22.627Z\n\tat ru.yandex.chemodan.uploader.registry.RequestDirectorUtils.failIfUserDataUploadTimedOut(RequestDirectorUtils.java:199)\n\tat ru.yandex.chemodan.uploader.registry.RequestDirector.withHandleIncomingFileAndFinalize(RequestDirector.java:141)\n\tat ru.yandex.chemodan.uploader.registry.RequestDirector.act(RequestDirector.java:201)\n\tat ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord$PatchAtDefault.act(MpfsRequestRecord.java:149)\n\tat ru.yandex.chemodan.uploader.local.queue.LocalQueueProcessor$1.apply(LocalQueueProcessor.java:72)\n\tat ru.yandex.chemodan.uploader.local.queue.LocalQueueProcessor$1.apply(LocalQueueProcessor.java:65)\n\tat ru.yandex.chemodan.uploader.local.queue.LocalRequestQueue.withNextRecord(LocalRequestQueue.java:102)\n\tat ru.yandex.chemodan.uploader.local.queue.LocalQueueProcessor.execute(LocalQueueProcessor.java:65)\n\tat ru.yandex.misc.worker.spring.DelayingWorkerServiceBeanSupport$1.execute(DelayingWorkerServiceBeanSupport.java:24)\n\tat ru.yandex.misc.worker.Workers$2.executePeriodically(Workers.java:53)\n\tat ru.yandex.misc.worker.DelayingWorkerThread.execute(DelayingWorkerThread.java:46)\n\tat ru.yandex.misc.worker.WorkerThread$2.call(WorkerThread.java:72)\n\tat ru.yandex.misc.worker.WorkerThread.quietly(WorkerThread.java:48)\n\tat ru.yandex.misc.worker.WorkerThread.run1(WorkerThread.java:37)\n\tat ru.yandex.misc.worker.StoppableThread.run(StoppableThread.java:54)\n"
      }
    },
    "originalFile2" : {
      "@class" : ".State$Initial",
      "failedAttempts" : 0
    },
    "patchedFile" : {
      "@class" : ".State$Initial",
      "failedAttempts" : 0
    },
    "patchedPayloadInfo" : {
      "@class" : ".State$Initial",
      "failedAttempts" : 0
    },
    "postProcess" : {
      "commitFileInfo" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      },
      "fileMulcaUploadInfo" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      },
      "digestMulcaUploadInfo" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      },
      "commitFileUpload" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      },
      "previewImageStatus" : {
      },
      "antivirusResult2" : {
        "@class" : ".State$Initial",
        "failedAttempts" : 0
      }
    },
    "commitFinal" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-08-03T19:55:23+00:00",
        "end" : "2012-08-03T19:55:23+00:00"
      },
      "lastProgress" : [ ],
      "result" : {
        "callbackResponse" : [ {
          "statusCode" : 200,
          "statusLine" : "HTTP/1.1 200 OK",
          "response" : "null",
          "uri" : [ "http://mpfs.disk.yandex.net:80/service/kladun_callback?uid=98931931&oid=092bde6c3a353dc94f4847d9046d5ee7c4afdabd1ac06265712efc11860676c5" ]
        } ]
      }
    },
    "notCancelledByAdmin" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-08-03T19:25:22+00:00",
        "end" : "2012-08-03T19:25:22+00:00"
      },
      "lastProgress" : [ ],
      "result" : true
    },
    "internalError" : {
      "@class" : ".State$Success",
      "failedAttempts" : 0,
      "executionInterval" : {
        "start" : "2012-08-03T19:25:22+00:00",
        "end" : "2012-08-03T19:25:22+00:00"
      },
      "lastProgress" : [ ],
      "result" : false
    }
  }
}
