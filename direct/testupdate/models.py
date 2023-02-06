# -*- coding:utf-8 -*-
from django.db import models
from datetime import datetime

class ReadyVersion(models.Model):
    testserver = models.CharField( max_length = 20, db_index = True )
    version = models.CharField(max_length=200)
    class Meta:
        unique_together = (("testserver", "version"),)
    def __unicode__(self):
        return "%s: %s" % (self.testserver, self.version)

class UpdateRequest(models.Model):
    reqid = models.BigIntegerField(default=0)
    logtime = models.DateTimeField()
    testserver = models.CharField( max_length = 20 )
    user = models.CharField(max_length=20)
    version = models.CharField(max_length=200)
    def save(self, *args, **kwargs):
            self.logtime = datetime.now()
            super(UpdateRequest, self).save(*args, **kwargs)
    def __unicode__(self):
        return "%s: %s %s" % (self.testserver, self.version, self.logtime)

class TestUpdateLog(models.Model):
    reqid = models.BigIntegerField(db_index = True, default=0)
    logtime = models.DateTimeField(db_index = True)
    testserver = models.CharField( max_length = 20 )
    version = models.CharField(max_length=200)
    status = models.IntegerField(default=0)
    logtext = models.TextField(max_length=32*1024*1024, null=True)
    def save(self, *args, **kwargs):
            self.logtime = datetime.now()
            super(TestUpdateLog, self).save(*args, **kwargs)
    def __unicode__(self):
        return "%s: %s %s" % (self.testserver, self.version, self.logtime)

