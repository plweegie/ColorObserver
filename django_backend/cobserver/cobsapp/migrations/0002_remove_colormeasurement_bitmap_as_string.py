# Generated by Django 2.0 on 2017-12-11 01:14

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('cobsapp', '0001_initial'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='colormeasurement',
            name='bitmap_as_string',
        ),
    ]