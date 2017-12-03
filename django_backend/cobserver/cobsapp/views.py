from django.http import HttpResponse, JsonResponse
from django.views.decorators.csrf import csrf_exempt
from rest_framework.renderers import JSONRenderer
from rest_framework.parsers import JSONParser
from .models import ColorMeasurement
from .serializers import ColorSerializer


@csrf_exempt
def color_list(request):
    if request.method == 'GET':
        colors = ColorMeasurement.objects.all()
        serializer = ColorSerializer(colors, many=True)
        return JsonResponse(serializer.data, safe=False)

    elif request.method == 'POST':
        data = JSONParser.parse(request)
        serializer = ColorSerializer(data=data)
        if serializer.is_valid():
            serializer.save()
            return JsonResponse(serializer.data, status=201)
        return JsonResponse(serializer.errors, status=400)


@csrf_exempt
def color_detail(request, pk):
    try:
        color = ColorMeasurement.objects.get(pk=pk)
    except ColorMeasurement.DoesNotExist:
        return HttpResponse(status=404)

    if request.method == 'GET':
        serializer = ColorSerializer(color)
        return JsonResponse(serializer.data)

    elif request.method == 'PUT':
        data = JSONParser.parse(request)
        serializer = ColorSerializer(color, data=data)
        if serializer.is_valid():
            serializer.save()
            return JsonResponse(serializer.data)

    elif request.method == 'DELETE':
        color.delete()
        return HttpResponse(status=204)
