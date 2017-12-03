from rest_framework import serializers
from .models import ColorMeasurement


class ColorSerializer(serializers.ModelSerializer):
    class Meta:
        model = ColorMeasurement
        fields = ('id', 'hue', 'saturation', 'intensity', 'bitmap_as_string')