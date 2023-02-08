from django.db import models
import re
from django.core.exceptions import ValidationError
from django.utils.translation import gettext_lazy as _
USERNAME_RE = re.compile(r'^[a-zA-Z0-9]+$')

def validate_username(username):
    if len(username) < 4:
        raise ValidationError("Username is less than 4 chars")
    if not USERNAME_RE.match(username):
        raise ValidationError("Username shall only have alphanumeric characters")


def validate_pwd(pwd):
    if len(pwd) < 8:
        raise ValidationError("pwd should be least 8 chars")
    if not any(char.isdigit() for char in pwd):
        raise ValidationError("pwd should have at least 1 digit")
    if not any(char.isupper() for char in pwd):
        raise ValidationError("pwd should have at least 1 uppercase")
    if not any(char.islower() for char in pwd):
        raise ValidationError("pwd should have at least 1 lowercase")
    



# User class
class User(models.Model):
    username = models.CharField(max_length=200, unique=True, validators=[validate_username])
    password = models.TextField(validators=[validate_pwd])
    email = models.EmailField(max_length=200, unique=True)


# Post class
class Post(models.Model):
    creator = models.ForeignKey(User, on_delete=models.CASCADE)
    title = models.CharField(max_length=200)
    content = models.TextField()
