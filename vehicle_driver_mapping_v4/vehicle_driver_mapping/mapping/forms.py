from django import forms
from .models import Driver, Vehicle, Assignment

class DriverForm(forms.ModelForm):
    class Meta:
        model = Driver
        fields = ['name', 'email', 'phone', 'location']

class VehicleForm(forms.ModelForm):
    class Meta:
        model = Vehicle
        fields = ['make_model', 'license_plate']
        
class AssignmentForm(forms.ModelForm):
    class Meta:
        model = Assignment
        fields = ['driver', 'vehicle', 'start_time', 'end_time']
        widgets = {
            'start_time': forms.DateTimeInput(attrs={'type': 'datetime-local', 'format':'%Y-%m-%dT%H:%M:%S'}),
            'end_time': forms.DateTimeInput(attrs={'type': 'datetime-local', 'format':'%Y-%m-%dT%H:%M:%S'}),
        }
