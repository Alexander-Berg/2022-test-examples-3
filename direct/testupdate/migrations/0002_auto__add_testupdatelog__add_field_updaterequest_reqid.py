# encoding: utf-8
import datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):

        # Adding model 'TestUpdateLog'
        db.create_table('testupdate_testupdatelog', (
            ('id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('reqid', self.gf('django.db.models.fields.BigIntegerField')(default=0, db_index=True)),
            ('logtime', self.gf('django.db.models.fields.DateTimeField')(db_index=True)),
            ('testserver', self.gf('django.db.models.fields.CharField')(max_length=20)),
            ('version', self.gf('django.db.models.fields.CharField')(max_length=200)),
            ('logtext', self.gf('django.db.models.fields.TextField')(max_length=33554432, null=True)),
        ))
        db.send_create_signal('testupdate', ['TestUpdateLog'])

        # Adding field 'UpdateRequest.reqid'
        db.add_column('testupdate_updaterequest', 'reqid', self.gf('django.db.models.fields.BigIntegerField')(default=0), keep_default=False)

    def backwards(self, orm):

        # Deleting model 'TestUpdateLog'
        db.delete_table('testupdate_testupdatelog')

        # Deleting field 'UpdateRequest.reqid'
        db.delete_column('testupdate_updaterequest', 'reqid')

    models = {
        'testupdate.readyversion': {
            'Meta': {'object_name': 'ReadyVersion'},
            'testserver': ('django.db.models.fields.CharField', [], {'max_length': '20', 'primary_key': 'True'}),
            'version': ('django.db.models.fields.CharField', [], {'max_length': '200'})
        },
        'testupdate.testupdatelog': {
            'Meta': {'object_name': 'TestUpdateLog'},
            'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'logtext': ('django.db.models.fields.TextField', [], {'max_length': '33554432', 'null': 'True'}),
            'logtime': ('django.db.models.fields.DateTimeField', [], {'db_index': 'True'}),
            'reqid': ('django.db.models.fields.BigIntegerField', [], {'default': '0', 'db_index': 'True'}),
            'testserver': ('django.db.models.fields.CharField', [], {'max_length': '20'}),
            'version': ('django.db.models.fields.CharField', [], {'max_length': '200'})
        },
        'testupdate.updaterequest': {
            'Meta': {'object_name': 'UpdateRequest'},
            'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'logtime': ('django.db.models.fields.DateTimeField', [], {}),
            'reqid': ('django.db.models.fields.BigIntegerField', [], {'default': '0'}),
            'testserver': ('django.db.models.fields.CharField', [], {'max_length': '20'}),
            'user': ('django.db.models.fields.CharField', [], {'max_length': '20'}),
            'version': ('django.db.models.fields.CharField', [], {'max_length': '200'})
        }
    }

    complete_apps = ['testupdate']
