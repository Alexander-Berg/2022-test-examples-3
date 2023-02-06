from django.conf.urls import patterns, url, include

urlpatterns = patterns('releaser.cloud_testupdate.views',
    (r'^update$', 'update'),
    (r'/?$', 'index'),
)
