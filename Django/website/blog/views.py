from django.shortcuts import render
from blog.models import User, Post
from django.forms import ModelForm
from django.http import HttpResponseRedirect, HttpResponse
from django.urls import reverse
from django.core.exceptions import ValidationError


def index(request):
    context = {'course': "CSE-60770", 'semester': 'SP23'}
    return render(request, 'blog/index.html', context)


def login(request):
    return render(request, 'blog/login.html')


class UserForm(ModelForm):
    class Meta:
        model = User
        fields = '__all__'


def register(request):
    errors = None
    if request.POST:
        # Create a model instance and populate it with data from the request
        uname = request.POST["username"]
        pwd = request.POST["password"]
        email = request.POST["email"]
        user = User(username=uname, password=pwd, email=email)

        try:
            user.full_clean()
            # if we reach here, the validation succeeded
            user.save()  # saves on the db
            # redirect to a new URL:
            return HttpResponseRedirect(reverse('blog:list_posts'))
        except ValidationError as e:
            errors = e

    return render(request, 'blog/register.html', {'errors': errors})


def list_posts(request):
    return HttpResponse("TBD")
