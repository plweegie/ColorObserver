from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.parsers import JSONParser
from rest_framework.response import Response

from .models import ColorMeasurement
from .serializers import ColorSerializer


@api_view(['GET', 'POST'])
def color_list(request, format=None):
    if request.method == 'GET':
        colors = ColorMeasurement.objects.all()
        serializer = ColorSerializer(colors, many=True)
        return Response(serializer.data)

    elif request.method == 'POST':
        data = JSONParser.parse(request)
        serializer = ColorSerializer(data=data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status.HTTP_201_CREATED)
        return Response(serializer.errors, status.HTTP_400_BAD_REQUEST)


@api_view(['GET', 'PUT', 'DELETE'])
def color_detail(request, pk, format=None):
    try:
        color = ColorMeasurement.objects.get(pk=pk)
    except ColorMeasurement.DoesNotExist:
        return Response(status.HTTP_404_NOT_FOUND)

    if request.method == 'GET':
        serializer = ColorSerializer(color)
        return Response(serializer.data)

    elif request.method == 'PUT':
        serializer = ColorSerializer(color, data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)

    elif request.method == 'DELETE':
        color.delete()
        return Response(status.HTTP_204_NO_CONTENT)
