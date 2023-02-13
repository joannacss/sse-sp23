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
        user = User(username=username, password=pwd, email=email)
        try:
            user.full_clean()
            user.password = make_password(pwd)
            user.save()
            return HttpResponseRedirect(reverse("blog:login"))
        except ValidationError as e:
            return render(request, 'blog/register.html', {"errors": e})

    return render(request, 'blog/register.html')


def login(request):
    if request.POST:
        username = request.POST.get("username")
        password = request.POST.get("password")
        user = User.objects.filter(username=username) # returns a list
        if len(user) != 0 and check_password(password, user[0].password):
            request.session["user"] = user[0].username
            return HttpResponseRedirect(reverse("blog:list_posts"))

        return render(request, 'blog/login.html', {'errors': [('Authentication', 'Username/pwd combination didnt match'), ]})
    return render(request, 'blog/login.html')



def logout(request):
    del request.session["user"]
    return HttpResponseRedirect(reverse("blog:login"))


def create_post(request):
    if request.session.get("user"):
        if request.POST:
            title = request.POST.get("title")
            content = request.POST.get("content")
            user = User.objects.filter(username=request.session["user"])[0]
            post = Post(title=title, content=content, creator=user)
            try:
                print("before full clean")
                post.full_clean()
                print("after full clean")
                post.save()
                return HttpResponseRedirect(reverse("blog:list_posts"))
            except ValidationError as e:
                return render(request, 'blog/create.html', {'errors': e})
        else:
            return render(request, 'blog/create.html')

    return HttpResponseRedirect(reverse("blog:login"))



def list_posts(request):
    context = {
        "posts": Post.objects.all()
    }
    return render(request, 'blog/list.html', context)


def view_post(request, post_id):
    post = get_object_or_404(Post, pk=post_id)
    return render(request, 'blog/view.html', {'post': post})
