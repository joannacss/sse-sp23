from django.shortcuts import render
from django.http import HttpResponse

# Create your views here.
def helloView(request):
	context = {
		"name": "Prof. Santos",
		"student_list":["Jane", "Joe", "Jon"]
	}
	return render(request, "hello.html", context)

