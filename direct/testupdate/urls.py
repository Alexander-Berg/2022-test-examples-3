from django.conf.urls import patterns, url, include

urlpatterns = patterns('releaser.testupdate.views',
    (r'^update$', 'update'),
    (r'^ready$', 'ready'),
    (r'^aready$', 'ready_authorized'),
    (r'^steady$', 'steady'),
    (r'^go$', 'update'),
    (r'^done$', 'done'),
    (r'^logindex$', 'logindex'),
    (r'^showlog$', 'showlog'),
    (r'/?$', 'index'),
)
