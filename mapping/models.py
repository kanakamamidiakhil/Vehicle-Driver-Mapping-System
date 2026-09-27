from django.db import models
from django.core.exceptions import ValidationError

class Driver(models.Model):
    driver_id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=255)
    email = models.EmailField(unique=True)
    phone = models.CharField(max_length=15, unique=True)
    location = models.CharField(max_length=255, blank=True, null=True)

    def __str__(self):
        return self.name

class Vehicle(models.Model):
    vehicle_id = models.AutoField(primary_key=True)
    make_model = models.CharField(max_length=255)
    license_plate = models.CharField(max_length=20, unique=True)

    def __str__(self):
        return self.make_model

class Assignment(models.Model):
    driver = models.ForeignKey(Driver, on_delete=models.CASCADE)
    vehicle = models.ForeignKey(Vehicle, on_delete=models.CASCADE)
    start_time = models.DateTimeField()
    end_time = models.DateTimeField()
    assignment_request_status = models.CharField(max_length=10, default="SENT")

    def __str__(self):
        return f"{self.driver.name} assigned to {self.vehicle.make_model} from {self.start_time} to {self.end_time}"

    def clean(self):
        if self.assignment_request_status == "ACCEPTED" or self.assignment_request_status == "SENT":
            # Ensure that the assignment time range does not overlap with existing assignments for the same vehicle.
            overlapping_assignments = Assignment.objects.filter(
                vehicle=self.vehicle,
                start_time__lt=self.end_time,
                end_time__gt=self.start_time,
                assignment_request_status="ACCEPTED"
            ).exclude(id=self.id)

            if overlapping_assignments.exists():
                raise ValidationError(f"This vehicle is already assigned during the time range {self.start_time} to {self.end_time}.")

            # Ensure that the driver is not assigned any car during assignment time range
            overlapping_assignments = Assignment.objects.filter(
                driver=self.driver,
                start_time__lt=self.end_time,
                end_time__gt=self.start_time,
                assignment_request_status="ACCEPTED"
            ).exclude(id=self.id)

            if overlapping_assignments.exists():
                raise ValidationError(
                    f"The driver is already busy during the time range {self.start_time} to {self.end_time}.")
    
    def save(self, *args, **kwargs):
        self.clean()  # Call clean method before saving to validate data.
        super().save(*args, **kwargs)
