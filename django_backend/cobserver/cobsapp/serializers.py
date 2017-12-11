from rest_framework import serializers
from .models import ColorMeasurement


class ColorSerializer(serializers.ModelSerializer):
    class Meta:
        model = ColorMeasurement
        fields = ('id', 'created', 'hue', 'saturation', 'intensity')
