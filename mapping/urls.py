from django.contrib import admin
from django.urls import path
from mapping import views  # Replace 'your_app' with the actual app name

urlpatterns = [
    #path('admin/', admin.site.urls),
    path('driver/', views.get_driver_page, name='driver_page'),
    path('driver/create_driver/', views.create_driver, name='create_driver'),
    path('driver/login/', views.driver_login, name='driver_login'),
    path('driver/assignments/', views.driver_assignments, name='driver_assignments'),
    path('driver/accept/<int:assignment_id>/', views.accept_assignment, name='accept_assignment'),
    path('driver/reject/<int:assignment_id>/', views.reject_assignment, name='reject_assignment'),

    path('admin/', views.get_admin_page, name='get_admin_page'),
    path('admin/create_vehicle/', views.create_vehicle, name='create_vehicle'),
    path('admin/search_drivers/', views.search_drivers, name='search_drivers'),
    path('admin/create_assignment/', views.create_assignment, name='create_assignment'),
    path('admin/driver_list/', views.driver_list, name='driver_list'),
    path('admin/vehicle_list/', views.vehicle_list, name='vehicle_list'),
    path('admin/assignment_list/', views.assignment_list, name='assignment_list'),
    path('admin/assignments/', views.assignments, name='assignments'),
    path('admin/unassign/<int:assignment_id>/', views.unassign_vehicle, name='unassign_vehicle'),

    path('users/nearby_drivers/', views.nearby_drivers, name='nearby_drivers'),
]
