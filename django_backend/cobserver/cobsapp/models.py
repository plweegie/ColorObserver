from django.db import models


class ColorMeasurement(models.Model):
    created = models.DateTimeField(auto_now_add=True)
    hue = models.FloatField()
    saturation = models.FloatField()
    intensity = models.FloatField()
