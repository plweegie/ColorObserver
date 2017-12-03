from django.conf.urls import url
from . import views

app_name = 'cobsapp'
urlpatterns = [
    url('', views.color_list),
    url('<int:pk>/', views.color_detail),
]