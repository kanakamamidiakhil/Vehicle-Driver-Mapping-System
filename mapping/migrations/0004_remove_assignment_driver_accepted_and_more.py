# Generated by Django 4.2.15 on 2024-08-12 20:55

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('mapping', '0003_assignment_driver_accepted_alter_driver_phone'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='assignment',
            name='driver_accepted',
        ),
        migrations.AddField(
            model_name='assignment',
            name='assignment_request_status',
            field=models.CharField(default='SENT', max_length=10),
        ),
    ]
