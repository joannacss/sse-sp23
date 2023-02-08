from django.shortcuts import render
from blog.models import User, Post
from django.forms import ModelForm
from django.http import HttpResponseRedirect, HttpResponse
from django.urls import reverse
from django.core.exceptions import ValidationError
from django.contrib.auth.hashers import make_password, check_password


def index(request):
    if request.session.get("user", None):
        return HttpResponseRedirect(reverse("blog:list_posts"))
    return HttpResponseRedirect(reverse("blog:login"))


# https://stackoverflow.com/a/43793754
def login(request):
    errors = None
    if request.POST:
        # Create a model instance and populate it with data from the request
        uname = request.POST["username"]
        pwd = request.POST["password"]
        user = User.objects.filter(username=uname)

        if len(user) > 0 and check_password(pwd, user[0].password):
            # create a new session
            request.session["user"] = uname
            return HttpResponseRedirect(reverse('blog:list_posts'))
        else:
            errors = [('authentication', "Login error")]

    return render(request, 'blog/login.html', {'errors': errors})


def logout(request):
    # remove the logged-in user information
    del request.session["user"]
    return HttpResponseRedirect(reverse("blog:login"))


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
            user.password = make_password(pwd)  # encrypts
            # if we reach here, the validation succeeded
            user.save()  # saves on the db
            # redirect to the login page
            return HttpResponseRedirect(reverse('blog:login'))
        except ValidationError as e:
            errors = e

    return render(request, 'blog/register.html', {'errors': errors})


def list_posts(request):
    context = {
        "posts": Post.objects.all()
    }
    return render(request, 'blog/list.html')
