from django.db import models
import re
from django.core.exceptions import ValidationError
from django.utils.translation import gettext_lazy as _

USERNAME_REGEX = re.compile(r'^[a-zA-Z0-9]+$')
PASSWORD_REGEX = re.compile(r'^[a-zA-Z0-9]+$')


# validator for usernames
def validate_username(username):
    if not USERNAME_REGEX.match(username) or len(username) < 4:
        raise ValidationError(
            _('%(username)s is not a valid username. Usernames shall have at least 4 alphanumeric characters'),
            params={'username': username},
        )


def validate_password(password):
    if len(password) < 8:
        raise ValidationError(_("Password must be at least 8 characters long."), code='invalid')
    if not any(char.isdigit() for char in password):
        raise ValidationError(_("Password must contain at least one number."), code='invalid')
    if not any(char.isupper() for char in password):
        raise ValidationError(_("Password must contain at least one uppercase letter."), code='invalid')
    if not any(char.islower() for char in password):
        raise ValidationError(_("Password must contain at least one lowercase letter."), code='invalid')


class User(models.Model):
    username = models.CharField(max_length=200, validators=[validate_username])
    password = models.CharField(max_length=64, validators=[validate_password])
    email = models.EmailField(max_length=200)
    #
    # def __str__(self):
    #     return self.username


class Post(models.Model):
    creator = models.ForeignKey(User, on_delete=models.CASCADE)
    title = models.CharField(max_length=200)
    content = models.TextField()
