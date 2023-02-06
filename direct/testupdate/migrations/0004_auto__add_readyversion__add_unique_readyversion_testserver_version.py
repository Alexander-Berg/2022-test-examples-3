# encoding: utf-8
import datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):

        # Adding model 'ReadyVersion'
        db.create_table('testupdate_readyversion', (
            ('id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('testserver', self.gf('django.db.models.fields.CharField')(max_length=20, db_index=True)),
            ('version', self.gf('django.db.models.fields.CharField')(max_length=200)),
        ))
        db.send_create_signal('testupdate', ['ReadyVersion'])

        # Adding unique constraint on 'ReadyVersion', fields ['testserver', 'version']
        db.create_unique('testupdate_readyversion', ['testserver', 'version'])

    def backwards(self, orm):

        # Removing unique constraint on 'ReadyVersion', fields ['testserver', 'version']
        db.delete_unique('testupdate_readyversion', ['testserver', 'version'])

        # Deleting model 'ReadyVersion'
        db.delete_table('testupdate_readyversion')

    models = {
        'testupdate.readyversion': {
            'Meta': {'unique_together': "(('testserver', 'version'),)", 'object_name': 'ReadyVersion'},
            'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'testserver': ('django.db.models.fields.CharField', [], {'max_length': '20', 'db_index': 'True'}),
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
