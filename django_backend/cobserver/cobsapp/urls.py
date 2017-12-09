from django.conf.urls import url
from rest_framework.urlpatterns import format_suffix_patterns
from . import views

app_name = 'cobsapp'
urlpatterns = [
    url('', views.color_list),
    url('<int:pk>/', views.color_detail),
]

urlpatterns = format_suffix_patterns(urlpatterns)