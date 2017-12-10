from rest_framework import generics

from .models import ColorMeasurement
from .serializers import ColorSerializer


class ColorList(generics.ListCreateAPIView):
    queryset = ColorMeasurement.objects.all()
    serializer_class = ColorSerializer


class ColorDetail(generics.RetrieveUpdateDestroyAPIView):
    queryset = ColorMeasurement.objects.all()
    serializer_class = ColorSerializer