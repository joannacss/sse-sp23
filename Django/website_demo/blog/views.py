from django.shortcuts import render
from django.shortcuts import get_object_or_404
from blog.models import User, Post
from django.forms import ModelForm
from django.http import HttpResponseRedirect, HttpResponse
from django.urls import reverse
from django.core.exceptions import ValidationError
from django.contrib.auth.hashers import make_password, check_password


def index(request):
    context = {'course': "CSE-60770", 'semester': 'SP23'}
    return render(request, 'blog/index.html', context)

# /blog/register
def register(request):
    if request.POST:
        # parameters
        username = request.POST.get("username")
        pwd = request.POST.get("password")
        email = request.POST.get("email")
        #  validate?
        user = User(username=username, password = pwd, email = email)
        try:
            user.full_clean()
            user.password = make_password(pwd)
            user.save()
            return HttpResponseRedirect(reverse("blog:login"))
        except ValidationError as e:
            return render(request, 'blog/register.html', {"errors": e})


    return render(request, 'blog/register.html')


def login(request):
    pass


def logout(request):
    pass




def create_post(request):
    pass


def list_posts(request):
    context = {
        "posts": Post.objects.all()
    }
    return render(request, 'blog/list.html', context)


def view_post(request, post_id):
    post = get_object_or_404(Post, pk=post_id)
    return render(request, 'blog/view.html', {'post': post})
