from datetime import datetime
from math import sqrt

from django.core.exceptions import ValidationError
from django.shortcuts import render, redirect, get_object_or_404
from .forms import DriverForm, VehicleForm, AssignmentForm
from .models import Driver, Vehicle, Assignment


def get_admin_page(request):
    return render(request, 'base.html')


def get_driver_page(request):
    return render(request, 'base_driver.html')


def driver_login(request):
    if request.method == 'POST':
        email = request.POST.get('email')
        password = request.POST.get('password')
        driver = Driver.objects.filter(email=email).first()
        if driver is not None:
            assignments = Assignment.objects.filter(driver=driver,  assignment_request_status__in=['SENT', 'ACCEPTED'])
            return render(request, 'driver_assignments.html', {'driver': driver, 'assignments': assignments})
        else:
            return render(request, 'driver_login.html', {'error': 'Invalid email or password'})
    return render(request, 'driver_login.html')


def driver_assignments(request):
    driver = request.user
    assignments = Assignment.objects.filter(driver=driver, assignment_request_status__in=['SENT', 'ACCEPTED'])
    return render(request, 'driver_assignments.html', {'driver': driver, 'assignments': assignments})


def accept_assignment(request, assignment_id):
    assignment = get_object_or_404(Assignment, id=assignment_id)
    assignment.assignment_request_status = 'ACCEPTED'
    try:
        assignment.save()
    except ValidationError as e:
        assignments = Assignment.objects.filter(driver=assignment.driver, assignment_request_status__in=['SENT', 'ACCEPTED'])
        return render(request, 'driver_assignments.html', {
            'driver': assignment.driver,
            'assignments': assignments,
            'error': e.message
        })
    assignments = Assignment.objects.filter(driver=assignment.driver, assignment_request_status__in=['SENT', 'ACCEPTED'])
    return render(request, 'driver_assignments.html', {'driver': assignment.driver, 'assignments': assignments})

def reject_assignment(request, assignment_id):
    assignment = get_object_or_404(Assignment, id=assignment_id)
    assignment.assignment_request_status = 'REJECTED'
    assignment.save()
    assignments = Assignment.objects.filter(driver=assignment.driver, assignment_request_status__in=['SENT', 'ACCEPTED'])
    return render(request, 'driver_assignments.html', {'driver': assignment.driver, 'assignments': assignments})




def create_driver(request):
    if request.method == 'POST':
        form = DriverForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('driver_login')
    else:
        form = DriverForm()
    return render(request, 'create_driver.html', {'form': form})


def create_vehicle(request):
    if request.method == 'POST':
        form = VehicleForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('vehicle_list')
    else:
        form = VehicleForm()
    return render(request, 'create_vehicle.html', {'form': form})


def driver_list(request):
    drivers = Driver.objects.all()
    return render(request, 'driver_list.html', {'drivers': drivers})


def vehicle_list(request):
    vehicles = Vehicle.objects.all()
    return render(request, 'vehicle_list.html', {'vehicles': vehicles})


def create_assignment(request):
    if request.method == 'POST':
        form = AssignmentForm(request.POST)
        if form.is_valid():
            form.save()
            return redirect('assignment_list')  # Ensure 'assignment_list' matches your URL pattern name
    else:
        form = AssignmentForm()
    return render(request, 'create_assignment.html', {'form': form})


def assignment_list(request):
    assignments = Assignment.objects.all()
    return render(request, 'assignment_list.html', {'assignments': assignments})


def assignments(request):
    driver = None
    assignments = []
    if request.method == 'POST':
        search_type = request.POST.get('type')
        search_term = request.POST.get('search_term')
        request_status = request.POST.get('request_status')
        drivers=[]
        if search_type == 'name':
            drivers = Driver.objects.filter(name=search_term)
        elif search_type == 'phone':
            drivers = Driver.objects.filter(phone=search_term)

        if len(drivers)>0:
            assignments = Assignment.objects.filter(driver__in=drivers, assignment_request_status=request_status)

    return render(request, 'assignments.html', {'assignments': assignments})


def unassign_vehicle(request, assignment_id):
    assignment = get_object_or_404(Assignment, id=assignment_id)
    assignment.delete()
    search_type = request.POST.get('type')
    search_term = request.POST.get('search_term')
    request_status = request.POST.get('request_status')

    drivers = []
    if search_type == 'name':
        drivers = Driver.objects.filter(name=search_term)
    elif search_type == 'phone':
        drivers = Driver.objects.filter(phone=search_term)

    if len(drivers) > 0:
        assignments = Assignment.objects.filter(driver__in=drivers, assignment_request_status=request_status)

    return render(request, 'assignments.html', {'assignments': assignments})


def search_drivers(request):
    drivers = []
    if request.method == 'POST':
        search_type = request.POST.get('type')
        search_term = request.POST.get('search_term')
        drivers = []
        if search_type == 'name':
            drivers = Driver.objects.filter(name=search_term)
        elif search_type == 'phone':
            drivers = Driver.objects.filter(phone=search_term)

    return render(request, 'get_driver_details.html', {'drivers': drivers})


def nearby_drivers(request):
    drivers = []
    if request.method == 'POST':
        x = float(request.POST.get('x'))
        y = float(request.POST.get('y'))
        proximity_range = float(request.POST.get('proximity_range'))
        datetime_str = request.POST.get('datetime')
        input_datetime = datetime.strptime(datetime_str, '%Y-%m-%dT%H:%M')

        # Filter drivers based on proximity and assignment status
        all_drivers = Driver.objects.all()
        driver_distances = []
        for driver in all_drivers:
            driver_x, driver_y = map(float, driver.location.split(','))
            distance = sqrt((driver_x - x) ** 2 + (driver_y - y) ** 2)
            if distance <= proximity_range:
                assignments = Assignment.objects.filter(
                    driver=driver,
                    assignment_request_status='ACCEPTED',
                    start_time__lte=input_datetime,
                    end_time__gte=input_datetime
                )
                if assignments.exists():
                    driver_distances.append((driver, distance))

        # Sort drivers by distance
        driver_distances.sort(key=lambda x: x[1])
        drivers = [driver for driver, distance in driver_distances]

    return render(request, 'nearby_drivers.html', {'drivers': drivers})
