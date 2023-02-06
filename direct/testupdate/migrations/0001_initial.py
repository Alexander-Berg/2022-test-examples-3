# encoding: utf-8
import datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):

        # Adding model 'ReadyVersion'
        db.create_table('testupdate_readyversion', (
            ('testserver', self.gf('django.db.models.fields.CharField')(max_length=20, primary_key=True)),
            ('version', self.gf('django.db.models.fields.CharField')(max_length=200)),
        ))
        db.send_create_signal('testupdate', ['ReadyVersion'])

        # Adding model 'UpdateRequest'
        db.create_table('testupdate_updaterequest', (
            ('id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('logtime', self.gf('django.db.models.fields.DateTimeField')()),
            ('testserver', self.gf('django.db.models.fields.CharField')(max_length=20)),
            ('user', self.gf('django.db.models.fields.CharField')(max_length=20)),
            ('version', self.gf('django.db.models.fields.CharField')(max_length=200)),
        ))
        db.send_create_signal('testupdate', ['UpdateRequest'])

    def backwards(self, orm):

        # Deleting model 'ReadyVersion'
        db.delete_table('testupdate_readyversion')

        # Deleting model 'UpdateRequest'
        db.delete_table('testupdate_updaterequest')

    models = {
        'testupdate.readyversion': {
            'Meta': {'object_name': 'ReadyVersion'},
            'testserver': ('django.db.models.fields.CharField', [], {'max_length': '20', 'primary_key': 'True'}),
            'version': ('django.db.models.fields.CharField', [], {'max_length': '200'})
        },
        'testupdate.updaterequest': {
            'Meta': {'object_name': 'UpdateRequest'},
            'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'logtime': ('django.db.models.fields.DateTimeField', [], {}),
            'testserver': ('django.db.models.fields.CharField', [], {'max_length': '20'}),
            'user': ('django.db.models.fields.CharField', [], {'max_length': '20'}),
            'version': ('django.db.models.fields.CharField', [], {'max_length': '200'})
        }
    }

    complete_apps = ['testupdate']
