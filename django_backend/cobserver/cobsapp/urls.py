from django.conf.urls import url
from rest_framework.urlpatterns import format_suffix_patterns
from . import views

app_name = 'cobsapp'
urlpatterns = [
    url(r'^colors/$', views.ColorList.as_view()),
    url(r'^colors/(?P<pk>[0-9]+)$', views.ColorDetail.as_view(), name='color-detail'),
]

urlpatterns = format_suffix_patterns(urlpatterns)