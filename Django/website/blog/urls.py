from django.urls import path

from . import views
app_name = 'blog'  # creates a namespace for this app
urlpatterns = [
    path('', views.index, name='index'),
    path('register', views.register, name='register'),
    path('login', views.login, name='login'),
    path('logout', views.logout, name='logout'),

    path('post/create', views.create_post, name='create_post'),
    path('post/<int:post_id>', views.view_post, name='view_post'),
    path('post/', views.list_posts, name='list_posts'),
]
